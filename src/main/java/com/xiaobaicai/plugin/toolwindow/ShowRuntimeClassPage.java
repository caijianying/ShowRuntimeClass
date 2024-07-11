package com.xiaobaicai.plugin.toolwindow;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBScrollPane;
import com.xiaobaicai.plugin.dialog.CompletionProvider;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import com.xiaobaicai.plugin.model.MatchedVmReturnModel;
import com.xiaobaicai.plugin.scan.FileScanner;
import com.xiaobaicai.plugin.tree.FileTree;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.function.Function;


/**
 * @author caijy
 * @description
 * @date 2024/3/18 星期一 2:42 上午
 */
public class ShowRuntimeClassPage {
    private Project project;

    private Function<MatchedVmModel, MatchedVmReturnModel> callback;

    /**
     * 查找启动类输入框
     **/
    public TextFieldWithAutoCompletion mainClassAutoCompletion;
    /**
     * 查找class输入框
     **/
    public TextFieldWithAutoCompletion classNamesCompletion;
    private JPanel jPanel = new JPanel(new BorderLayout());
    private CompletionProvider completionProvider;
    private FileTree fileTree;
    private FileScanner fileScanner;

    public ShowRuntimeClassPage(Project project, Function<MatchedVmModel, MatchedVmReturnModel> callback) {
        this.project = project;
        this.callback = callback;
        this.fileScanner = FileScanner.INSTANCE;
        this.completionProvider = new CompletionProvider(this.fileScanner.compare(project));
    }

    public JPanel createUIComponents() {
        ApplicationManager.getApplication().invokeLater(() -> {
            this.completionProvider.setItems(this.fileScanner.compare(project));
        });
        // 查找启动类输入框
        mainClassAutoCompletion = new TextFieldWithAutoCompletion(this.project, completionProvider, true, null);
        mainClassAutoCompletion.addDocumentListener(new DocumentListener() {
            @SneakyThrows
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                String showName = event.getDocument().getText();
                System.out.println("textFieldWithAutoCompletion.documentChanged: " + showName);
                completionProvider.handleMainClassChoosed(showName, callback, fileTree);
            }
        });
        mainClassAutoCompletion.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    completionProvider.setItems(fileScanner.compare(project));
                });
                System.out.println("mainClassAutoCompletion.focusLost");
            }

            @Override
            public void focusGained(FocusEvent e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    completionProvider.setItems(fileScanner.compare(project));
                });
                System.out.println("mainClassAutoCompletion.focusGained");
            }
        });
        mainClassAutoCompletion.setPreferredSize(new Dimension(250, 30));

        JPanel topPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        List<String> showNames = Lists.newArrayList();
        topPane.add(new JLabel("Main Class"));
        topPane.add(mainClassAutoCompletion);

        // 创建文件树
        EditorFactory editorFactory = EditorFactory.getInstance();
        DocumentImpl document = new DocumentImpl("");
        EditorEx editor = (EditorEx) editorFactory.createEditor(document, project);
        JBScrollPane scrollPane = new JBScrollPane(editor.getComponent());
        fileTree = new FileTree(project, editor, newShowName -> {
            showNames.add(newShowName.replace("/", "."));
        });
        JPanel treeJPanel = new JPanel(new BorderLayout());


        // 查找class输入框
        classNamesCompletion = new TextFieldWithAutoCompletion(this.project, new TextFieldWithAutoCompletion.StringsCompletionProvider(fileScanner.getClassNames(), IconLoader.findIcon("./icons/show.svg")), true, null);
        classNamesCompletion.addDocumentListener(new DocumentListener() {
            @Override
            public void beforeDocumentChange(@NotNull DocumentEvent event) {
                System.out.println("classNamesCompletion.beforeDocumentChange: " + event.getDocument().getText());
                ApplicationManager.getApplication().invokeLater(() -> {
                    classNamesCompletion.setVariants(showNames);
                });
            }

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                String showName = event.getDocument().getText();
                System.out.println("classNamesCompletion.documentChanged: " + showName);
                String nodePath = showName.replace(".", "/");
                ApplicationManager.getApplication().invokeLater(() -> {
                    fileTree.setSelectedNode(nodePath);
                });
            }
        });
        classNamesCompletion.setPreferredSize(new Dimension(250, 30));

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        ToolbarPanel refreshPanel = new ToolbarPanel(classNamesCompletion, actionGroup);
        refreshPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        refreshPanel.add(new JLabel(IconLoader.findIcon("./icons/show.svg")));
        refreshPanel.add(classNamesCompletion);

        treeJPanel.add(refreshPanel, BorderLayout.NORTH);
        treeJPanel.add(fileTree, BorderLayout.CENTER);
        JBScrollPane treeScrollPane = new JBScrollPane(treeJPanel);
        treeScrollPane.setColumnHeaderView(refreshPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, scrollPane);
        splitPane.setResizeWeight(0.3);

        this.jPanel.add(topPane, BorderLayout.NORTH);
        this.jPanel.add(splitPane, BorderLayout.CENTER);
        this.jPanel.revalidate();
        this.jPanel.repaint();

        return this.jPanel;
    }
}
