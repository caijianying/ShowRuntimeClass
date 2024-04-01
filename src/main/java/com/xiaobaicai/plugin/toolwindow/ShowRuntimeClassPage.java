package com.xiaobaicai.plugin.toolwindow;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.ui.GotItTooltip;
import com.intellij.ui.IconWrapperWithToolTip;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.roots.ToolbarPanel;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.xiaobaicai.plugin.dialog.CompletionProvider;
import com.xiaobaicai.plugin.model.ClassInfoModel;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import com.xiaobaicai.plugin.model.MatchedVmReturnModel;
import com.xiaobaicai.plugin.tree.FileTree;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
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

    private List<ClassInfoModel> mainClasses;

    private List<ClassInfoModel> allClasses;

    private List<String> classNames;

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

    public ShowRuntimeClassPage(Project project, Function<MatchedVmModel, MatchedVmReturnModel> callback) {
        this.project = project;
        this.callback = callback;
        this.mainClasses = Lists.newArrayList();
        this.allClasses = Lists.newArrayList();
        this.classNames = Lists.newArrayList();
        this.completionProvider = new CompletionProvider(this.compare(project));
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

            @SneakyThrows
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                String showName = event.getDocument().getText();
                System.out.println("textFieldWithAutoCompletion.documentChanged: " + showName);
                boolean matched = completionProvider.getShowNameVmMap().containsKey(showName);
                if (matched) {
                    MatchedVmModel vmModel = completionProvider.getShowNameVmMap().get(showName);
                    Future<MatchedVmReturnModel> modelFuture = ApplicationManager.getApplication().executeOnPooledThread(new Callable<MatchedVmReturnModel>() {
                        @Override
                        public MatchedVmReturnModel call() {
                            return callback.apply(vmModel);
                        }
                    });
                    fileTree.handleFutureVmProcessChoosed(modelFuture);
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

            @Override
            public void focusGained(FocusEvent e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    completionProvider.setItems(compare(project));
                });
                System.out.println("mainClassAutoCompletion.focusGained");
            }
        });
        mainClassAutoCompletion.setPreferredSize(new Dimension(250, 30));

        JPanel topPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        List<String> showNames = Lists.newArrayList();
        topPane.add(new JLabel("Main Class"));

//        String tipMessage = "这是一个IconWrapperWithToolTip";
//        IconWrapperWithToolTip toolTip = new IconWrapperWithToolTip(IconLoader.findIcon("./icons/show.svg"), () -> tipMessage);
//        JLabel problemIcon = new JLabel(toolTip);
//        problemIcon.setToolTipText(tipMessage);
//        topPane.add(problemIcon);

//        GotItTooltip gotItTooltip =  new GotItTooltip("got.it.id", "", project).
//                // 为了方便调试，设置为100，该提示会出现 100 次
//                        withShowCount(5).
//                // 引导提示内容
//                        withHeader("输入文本，点击翻译按钮即可完成翻译");
//
//        // 引导提示位置设置在翻译按钮的正下方位置
//        gotItTooltip.show(mainClassAutoCompletion, GotItTooltip.BOTTOM_MIDDLE);

//        new GotItTooltip("got.it.id", "", ).
//                // 为了方便调试，设置为100，该提示会出现 100 次
//                        withShowCount(3).
//                // 引导提示内容
//                        withHeader("输入当前项目下的启动类，插件会自动为你匹配到运行的进程！").
//                // 引导提示位置设置在翻译按钮的正下方位置
//                        show(mainClassAutoCompletion, GotItTooltip.BOTTOM_MIDDLE);
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
        classNamesCompletion = new TextFieldWithAutoCompletion(this.project, new TextFieldWithAutoCompletion.StringsCompletionProvider(classNames, IconLoader.findIcon("./icons/show.svg")), true, null);
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

        return jPanel;
    }


    private List<MatchedVmModel> compare(Project project) {
        // 扫描 mainClass
        List<ClassInfoModel> mainClasses = scan(project);
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

    private List<ClassInfoModel> scan(Project project) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        PsiManager psiManager = PsiManager.getInstance(project);
        for (Module module : modules) {
            if (!module.getName().equals(project.getName())) {
                File file = module.getModuleNioFile().toFile();
                if (file != null) {
                    scanFile(file.getParentFile(), psiManager, module);
                }

            }
        }
        return mainClasses;
    }

    private void scanFile(File file, PsiManager psiManager, Module module) {
        if (file == null) {
            return;
        }
        if (!file.isDirectory()) {
            if (file.getName().endsWith(".java")) {
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(file.getAbsolutePath());
                PsiJavaFile javaFile = (PsiJavaFile) psiManager.findFile(virtualFile);
                String className = javaFile.getPackageName() + "." + javaFile.getName().replace(".java", "");
                ClassInfoModel infoModel = new ClassInfoModel();
                infoModel.setClassName(className);
                infoModel.setModuleName(module.getName());
                if (this.hasMainMethod(javaFile)) {
                    if (!classNames.contains(className)) {
                        mainClasses.add(infoModel);
                    }
                    for (ClassInfoModel clazz : mainClasses) {
                        if (clazz.getClassName().equals(className)) {
                            clazz.setModuleName(module.getName());
                        }
                    }
                }
                classNames.add(className);
                allClasses.add(infoModel);
            }
            return;
        }

        File[] files = file.listFiles();
        for (File targetFile : files) {
            scanFile(targetFile, psiManager, module);
        }
    }

    /**
     * 是否含有main方法
     **/
    private boolean hasMainMethod(PsiJavaFile javaFile) {
        PsiClass[] classes = javaFile.getClasses();
        for (PsiClass psiClass : classes) {
            PsiMethod[] methods = psiClass.findMethodsByName("main", false);
            for (PsiMethod method : methods) {
                if (isMainMethod(method)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMainMethod(PsiMethod method) {
        if (!method.hasModifierProperty(PsiModifier.PUBLIC)) {
            return false;
        }
        if (!method.hasModifierProperty(PsiModifier.STATIC)) {
            return false;
        }
        if (!PsiType.VOID.equals(method.getReturnType())) {
            return false;
        }
        PsiParameterList parameterList = method.getParameterList();
        PsiParameter[] parameters = parameterList.getParameters();
        if (parameters.length != 1) {
            return false;
        }
        PsiType parameterType = parameters[0].getType();
        if (!(parameterType instanceof PsiArrayType)) {
            return false;
        }
        return "String[]".equals(parameterType.getPresentableText());
    }
}
