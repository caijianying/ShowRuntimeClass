package com.xiaobaicai.plugin.app.tree;

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
import com.intellij.ui.treeStructure.Tree;
import com.xiaobaicai.plugin.core.service.RemoteService;
import com.xiaobaicai.plugin.core.utils.RemoteUtil;
import lombok.SneakyThrows;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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

    public FileTree(Project project, EditorEx editor, Consumer<String> treeChangeCallback) {

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
                            String classFilePath = remoteService.retransFormClass(fullClassName);
                            if (classFilePath == null) {
                                System.out.println("classFilePath is null!");
                                return;
                            }
                            updateFileContent(classFilePath, editor, project);
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

    private void updateFileContent(String filePath, EditorEx editor, Project project) {
        // 在这里更新右侧文本区域的内容，根据文件名加载文件内容等操作
        System.out.println("Selected File: " + filePath);

        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (virtualFile != null) {
            ApplicationManager.getApplication().runWriteAction(() -> {
                Document document = FileDocumentManager.getInstance().getDocument(virtualFile);

                EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
                editor.setColorsScheme(colorsScheme);

                EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
                editor.setHighlighter(highlighterFactory.createEditorHighlighter(project, virtualFile));
                editor.setViewer(true);

                // 设置编辑器文本内容
                if (document != null) {
                    editor.getDocument().setText(document.getText());
                }
            });
        }
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
        this.repaint();
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

        if (match){
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
