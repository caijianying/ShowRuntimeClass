package com.xiaobaicai.plugin.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.xiaobaicai.plugin.core.service.RemoteService;
import com.xiaobaicai.plugin.core.utils.RemoteUtil;
import com.xiaobaicai.plugin.utils.PluginUtils;
import lombok.SneakyThrows;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

    private JBScrollPane scrollPane;

    private Integer port;

    private EditorEx editor;

    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 5, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    public FileTree(Project project, EditorEx editor, JBScrollPane scrollPane, Consumer<String> treeChangeCallback) {
        this.scrollPane = scrollPane;
        this.editor = editor;
        this.treeChangeCallback = treeChangeCallback;

        setModel(new DefaultTreeModel(root));
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        addMouseListener(new MouseAdapter() {
            @SneakyThrows
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) getLastSelectedPathComponent();
                    if (selectedNode != null && selectedNode.getChildCount() == 0) {
                        String fullClassName = getSelectedFullClassName(selectedNode);
                        RemoteService remoteService = RemoteUtil.getRemoteService(getPort());
                        if (remoteService != null) {
                            Component loadingComponent = PluginUtils.replaceEditorWithLoading(editor);
                            String classFilePath = remoteService.retransFormClass(fullClassName);
                            if (classFilePath == null) {
                                System.out.println("classFilePath is null!");
                                return;
                            }
                            updateFileContent(classFilePath, project, loadingComponent);
                            nodePathMap.put(fullClassName, classFilePath);
                        }

                    }
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

    private void updateFileContent(String filePath, Project project, Component loadingComponent) {
        // 在这里更新右侧文本区域的内容，根据文件名加载文件内容等操作
        System.out.println("Selected File: " + filePath);
        // 启动一个新线程来模拟加载文件内容的耗时操作
        threadPoolExecutor.execute(() -> {
            VirtualFile virtualFile = null;
            while (true) {
                virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath);
                if (virtualFile != null) {
                    break;
                }
            }
            // 文件加载完成后，在 UI 线程中更新内容和进度条状态
            VirtualFile finalVirtualFile = virtualFile;
            ApplicationManager.getApplication().invokeLater(() -> {
                loadingComponent.setVisible(false);
                // 设置编辑器文本内容
                ApplicationManager.getApplication().runWriteAction(() -> {
                    Document document = FileDocumentManager.getInstance().getDocument(finalVirtualFile);
                    EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
                    editor.setColorsScheme(colorsScheme);

                    EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
                    editor.setHighlighter(highlighterFactory.createEditorHighlighter(project, finalVirtualFile));
                    editor.setViewer(true);

                    // 设置编辑器文本内容
                    if (document != null) {
                        editor.getDocument().setText(document.getText());
                    }
                });
                PluginUtils.replaceLoadingWithEditor(editor);
            });

        });
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

    public void reset() {
        this.root.removeAllChildren();
        // 通知模型发生节点变化
        ((DefaultTreeModel) this.getModel()).reload(this.root);
        ApplicationManager.getApplication().runWriteAction(() -> {
            this.repaint();
            // 编辑器变为空
            editor.setViewer(true);
            editor.getDocument().setText("");
        });
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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}