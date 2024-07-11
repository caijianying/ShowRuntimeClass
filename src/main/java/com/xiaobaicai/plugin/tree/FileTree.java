package com.xiaobaicai.plugin.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.treeStructure.Tree;
import com.xiaobaicai.plugin.model.MatchedVmReturnModel;
import com.xiaobaicai.plugin.utils.MessageUtil;
import com.xiaobaicai.plugin.utils.PluginUtils;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @author caijy
 * @description
 * @date 2024/3/8 星期五 4:30 下午
 */
public class FileTree extends Tree {
    /**
     * 根节点
     **/
    private DefaultMutableTreeNode root = new DefaultMutableTreeNode("ShowRuntimeClass");

    private Map<String, String> nodePathMap = new HashMap<>();

    private Consumer<String> treeChangeCallback;

    private Integer port;

    private EditorEx editor;
    // 进度条
    private JProgressBar progressBar;
    // 显示进度条的对话框
    private JDialog progressDialog;

    public FileTree(Project project, EditorEx editor, Consumer<String> treeChangeCallback) {
        this.editor = editor;
        this.treeChangeCallback = treeChangeCallback;

        // 创建进度条
        progressBar = new JProgressBar();
        progressBar.setString("Loading...");
        progressBar.setStringPainted(true);

        // 创建对话框
        progressDialog = new JDialog((Frame) null, "Loading", true);
        // 不显示边框
        progressDialog.setUndecorated(true);
        progressDialog.add(progressBar);
        progressDialog.pack();
        // 将对话框显示在屏幕中央
        progressDialog.setLocationRelativeTo(editor.getComponent());

        setModel(new DefaultTreeModel(root));
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        addMouseListener(new MouseAdapter() {
            @SneakyThrows
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) getLastSelectedPathComponent();
                    boolean isLeafNode = (selectedNode != null && selectedNode.getChildCount() == 0);
                    if (!isLeafNode || selectedNode.isRoot()) {
                        return;
                    }
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            String fullClassName = getSelectedFullClassName(selectedNode);
                            String classFilePath = PluginUtils.retransformClassRemotely(getPort(), fullClassName);
                            if (classFilePath == null && nodePathMap.containsKey(fullClassName)) {
                                classFilePath = nodePathMap.get(fullClassName);
                            }
                            if (classFilePath != null) {
                                System.out.println(classFilePath + " selected.");
                                PluginUtils.updateEditorContent(classFilePath, editor, project);
                                nodePathMap.put(fullClassName, classFilePath);
                            }
                        } catch (Throwable ex) {
                            PluginUtils.handleError(ex);
                            return;
                        }
                        // 在 UI 线程中隐藏进度条
                        ApplicationManager.getApplication().invokeLater(() -> {
                            progressBar.setIndeterminate(false);
                            progressDialog.setVisible(false);
                        });
                    });

                    // 显示进度条
                    progressBar.setIndeterminate(true);
                    // 巨坑，这是一个阻塞操作！
                    // 显示进度条后，会导致后面的代码无法执行。所以预先用多线程执行隐藏进度条的代码
                    progressDialog.setVisible(true);
                }
            }
        });
    }

    private String getSelectedFullClassName(DefaultMutableTreeNode selectedNode) {
        String className = selectedNode.getUserObject().toString();
        LinkedList<String> fullClassNameList = new LinkedList<>();
        fullClassNameList.addFirst(className);
        String rootDirName = root.getUserObject().toString();
        while (true) {
            selectedNode = (DefaultMutableTreeNode) selectedNode.getParent();
            String dirName = selectedNode.getUserObject().toString();
            if (rootDirName.equals(dirName)) {
                break;
            }
            fullClassNameList.addFirst(selectedNode.getUserObject().toString());
        }
        return String.join(".", fullClassNameList);
    }

    public void addNode(String availableClass) {
        String className = availableClass.replace(".", "/");
        String[] dirs = className.split("\\/");
        DefaultMutableTreeNode parent = root;
        for (String dir : dirs) {
            parent = this.createNodeIfNecessary(parent, dir, className);
        }
        this.repaint();
    }

    private void reset() {
        this.root.removeAllChildren();
        // 通知模型发生节点变化
        ((DefaultTreeModel) this.getModel()).reload(this.root);
        ApplicationManager.getApplication().runWriteAction(() -> {
            this.repaint();
            // 编辑器变为空
            editor.setViewer(true);
            editor.getDocument().setText("");
        });
        setIconRender();
    }

    private void setIconRender() {
        DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
        cellRenderer.setLeafIcon(IconLoader.findIcon("./icons/classIcon.svg"));
        setCellRenderer(cellRenderer);
    }

    /**
     * @return dir对应的node
     **/
    private DefaultMutableTreeNode createNodeIfNecessary(DefaultMutableTreeNode parent, String dir, String className) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (dir.equals(child.getUserObject().toString())) {
                return child;
            }
        }

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(dir);
        parent.add(newNode);

        treeChangeCallback.accept(className);
        return newNode;
    }

    public void setSelectedNode(String nodePath) {
        String[] dirs = nodePath.split("/");
        DefaultMutableTreeNode curNode = this.root;

        boolean match = true;
        for (String dir : dirs) {
            boolean matchePath = false;
            for (int i = 0; i < curNode.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) curNode.getChildAt(i);
                if (child.getUserObject().equals(dir)) {
                    matchePath = true;
                    curNode = child;
                    break;
                }
            }
            if (!matchePath) {
                match = false;
                break;
            }
        }

        if (match) {
            TreePath path = new TreePath(curNode.getPath());
            this.setSelectionPath(path);
            this.repaint();
        }
    }

    private Integer getPort() {
        return port;
    }

    private void setPort(Integer port) {
        this.port = port;
    }

    public void handleFutureVmProcessChoosed(Future<MatchedVmReturnModel> modelFuture) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            MatchedVmReturnModel returnModel = null;
            try {
                returnModel = modelFuture.get();
            } catch (InterruptedException e) {
                PluginUtils.saveErrorLog(e);
            } catch (ExecutionException e) {
                PluginUtils.saveErrorLog(e);
            } catch (Throwable ex) {
                PluginUtils.saveErrorLog(ex);
            }
            if (returnModel == null) {
                MessageUtil.info("出现异常!");
                return;
            }
            Set<String> allAvailableClasses = returnModel.getClasses();
            if (allAvailableClasses != null) {
                this.setPort(returnModel.getPort());
                ApplicationManager.getApplication().invokeLater(() -> {
                    this.reset();

                    for (String availableClass : allAvailableClasses) {
                        this.addNode(availableClass);
                    }
                });
            }
        });
    }
}
