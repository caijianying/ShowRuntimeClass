package com.xiaobaicai.plugin.app.toolwindow;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.IconWrapperWithToolTip;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.roots.ToolbarPanel;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.xiaobaicai.plugin.app.dialog.CompletionProvider;
import com.xiaobaicai.plugin.app.model.MainClassInfoModel;
import com.xiaobaicai.plugin.app.model.MatchedVmModel;
import com.xiaobaicai.plugin.app.model.MatchedVmReturnModel;
import com.xiaobaicai.plugin.app.tree.FileTree;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author caijy
 * @description
 * @date 2024/3/18 星期一 2:42 上午
 */
public class ShowRuntimeClassPage {
    private Project project;

    private Function<MatchedVmModel, MatchedVmReturnModel> callback;

    private java.util.List<MainClassInfoModel> mainClasses;

    private List<String> classNames;

    // 查找启动类输入框
    private TextFieldWithAutoCompletion mainClassAutoCompletion;
    // 查找class输入框
    private TextFieldWithAutoCompletion classNamesCompletion;
    private JPanel jPanel = new JPanel(new BorderLayout());
    private CompletionProvider completionProvider;
    private FileTree fileTree;

    public ShowRuntimeClassPage(Project project, Function<MatchedVmModel, MatchedVmReturnModel> callback) {
        this.project = project;
        this.callback = callback;
        this.mainClasses = Lists.newArrayList();
        this.classNames = Lists.newArrayList();
        this.completionProvider = new CompletionProvider(this.compare(project), IconLoader.findIcon("./icons/show.svg"));
    }

    public JPanel createUIComponents() {
        ApplicationManager.getApplication().invokeLater(() -> {
            this.completionProvider.setItems(this.compare(project));
        });
        // 查找启动类输入框
        mainClassAutoCompletion = new TextFieldWithAutoCompletion(this.project, completionProvider, true, null);
        mainClassAutoCompletion.addDocumentListener(new DocumentListener() {
            @Override
            public void beforeDocumentChange(@NotNull DocumentEvent event) {
                System.out.println("textFieldWithAutoCompletion.beforeDocumentChange: " + event.getDocument().getText());
            }

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                String showName = event.getDocument().getText();
                System.out.println("textFieldWithAutoCompletion.documentChanged: " + showName);
                boolean matched = completionProvider.getShowNameVmMap().containsKey(showName);
                if (matched) {
                    MatchedVmModel vmModel = completionProvider.getShowNameVmMap().get(showName);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        MatchedVmReturnModel returnModel = callback.apply(vmModel);
                        Set<String> allAvailableClasses = returnModel.getClasses();
                        if (allAvailableClasses != null) {
                            fileTree.setPort(returnModel.getPort());
                            fileTree.reset();
                            for (String availableClass : allAvailableClasses) {
                                fileTree.addNode(availableClass);
                            }
                        }
                    });
                }
            }
        });
        mainClassAutoCompletion.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    completionProvider.setItems(compare(project));
                });
                System.out.println("mainClassAutoCompletion.focusLost");
            }
        });
        mainClassAutoCompletion.setPreferredSize(new Dimension(250, 30));
//        mainClassAutoCompletion.setPlaceholder("please enter your main class package like xx.xx.xx");

        JPanel topPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        List<String> showNames = Lists.newArrayList();
        topPane.add(new JLabel("Main Class"));

        String tipMessage = "这是一个IconWrapperWithToolTip";
        IconWrapperWithToolTip toolTip = new IconWrapperWithToolTip(IconLoader.findIcon("./icons/show.svg"), () -> tipMessage);
        JLabel problemIcon = new JLabel(toolTip);
        problemIcon.setToolTipText(tipMessage);
        topPane.add(problemIcon);
        topPane.add(mainClassAutoCompletion);


        // 创建文件树
        EditorFactory editorFactory = EditorFactory.getInstance();
        DocumentImpl document = new DocumentImpl("");
        EditorEx editor = (EditorEx) editorFactory.createEditor(document, project);
        JScrollPane jScrollPane = new JScrollPane(editor.getComponent());
        fileTree = new FileTree(project, editor, newShowName -> {
            showNames.add(newShowName);
        });
        JPanel treeJPanel = new JPanel(new BorderLayout());


        // 查找class输入框
        classNamesCompletion = new TextFieldWithAutoCompletion(this.project, new TextFieldWithAutoCompletion.StringsCompletionProvider(classNames, IconLoader.findIcon("./icons/show.svg")), true, null);
        classNamesCompletion.addDocumentListener(new DocumentListener() {
            @Override
            public void beforeDocumentChange(@NotNull DocumentEvent event) {
                System.out.println("classNamesCompletion.beforeDocumentChange: " + event.getDocument().getText());
            }

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                String showName = event.getDocument().getText();
                System.out.println("classNamesCompletion.documentChanged: " + showName);
                String nodePath = showName.replace(".", "/");
                if (showNames.contains(nodePath)) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        fileTree.setSelectedNode(nodePath);
                    });
                }
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
        JScrollPane treeScrollPane = new JScrollPane(treeJPanel);
        treeScrollPane.setColumnHeaderView(refreshPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, jScrollPane);
        splitPane.setResizeWeight(0.3);

        this.jPanel.add(topPane, BorderLayout.NORTH);
        this.jPanel.add(splitPane, BorderLayout.CENTER);
        this.jPanel.revalidate();
        this.jPanel.repaint();

        return jPanel;
    }


    private List<MatchedVmModel> compare(Project project) {
        // 扫描 mainClass
        List<MainClassInfoModel> mainClasses = scan(project);
        // 查找进程
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        // 匹配
        return Optional.ofNullable(mainClasses).orElse(Collections.emptyList()).stream().map(mainClass -> {
            MatchedVmModel vmModel = new MatchedVmModel();
            vmModel.setMainClass(mainClass.getClassName());
            vmModel.setRunning(0);
            vmModel.setModuleName(mainClass.getModuleName());
            VirtualMachineDescriptor matchedVm = list.stream().filter(vm -> vm.displayName().equals(mainClass.getClassName())).findFirst().orElse(null);
            if (matchedVm != null) {
                vmModel.setRunning(1);
                vmModel.setPid(matchedVm.id());
            }
            return vmModel;
        }).filter(vm -> vm.getRunning() == 1).collect(Collectors.toList());
    }

    private List<MainClassInfoModel> scan(Project project) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        PsiManager psiManager = PsiManager.getInstance(project);
        for (Module module : modules) {
            if (!module.getName().equals(project.getName())) {
                VirtualFile file = module.getModuleFile();
                scanFile(file.getParent(), psiManager, module);
            }
        }
        return mainClasses;
    }

    private void scanFile(VirtualFile file, PsiManager psiManager, Module module) {
        if (!file.isDirectory() && file.getName().endsWith(".java")) {
            String fileBody = null;
            try {
                fileBody = FileUtils.readFileToString(new File(file.getPath()), "UTF-8");
            } catch (IOException e) {

            }
            if (fileBody == null) {
                return;
            }
            PsiJavaFile javaFile = (PsiJavaFile) psiManager.findFile(file);
            String className = javaFile.getPackageName() + "." + javaFile.getName().replace(".java", "");
            if (fileBody.contains("public static void main")) {
                MainClassInfoModel infoModel = new MainClassInfoModel();
                infoModel.setClassName(className);
                infoModel.setModuleName(module.getName());
                mainClasses.add(infoModel);
            }
            classNames.add(className);
            return;
        }

        VirtualFile[] files = file.getChildren();
        for (VirtualFile virtualFile : files) {
            scanFile(virtualFile, psiManager, module);
        }
    }
}
