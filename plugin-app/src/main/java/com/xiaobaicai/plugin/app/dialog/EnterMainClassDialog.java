//package com.xiaobaicai.plugin.app.dialog;
//
//import com.google.common.collect.Lists;
//import com.intellij.openapi.actionSystem.DefaultActionGroup;
//import com.intellij.openapi.editor.EditorFactory;
//import com.intellij.openapi.editor.event.DocumentEvent;
//import com.intellij.openapi.editor.event.DocumentListener;
//import com.intellij.openapi.editor.ex.EditorEx;
//import com.intellij.openapi.editor.impl.DocumentImpl;
//import com.intellij.openapi.module.Module;
//import com.intellij.openapi.module.ModuleManager;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.DialogWrapper;
//import com.intellij.openapi.ui.ValidationInfo;
//import com.intellij.openapi.util.IconLoader;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.psi.PsiJavaFile;
//import com.intellij.psi.PsiManager;
//import com.intellij.ui.TextFieldWithAutoCompletion;
//import com.intellij.ui.roots.ToolbarPanel;
//import com.sun.tools.attach.VirtualMachine;
//import com.sun.tools.attach.VirtualMachineDescriptor;
//import com.xiaobaicai.plugin.app.model.MainClassInfoModel;
//import com.xiaobaicai.plugin.app.model.MatchedVmModel;
//import com.xiaobaicai.plugin.app.model.SelectCallBackModel;
//import com.xiaobaicai.plugin.app.service.RemoteAppService;
//import com.xiaobaicai.plugin.app.tree.FileTree;
//import com.xiaobaicai.plugin.core.service.RemoteService;
//import org.apache.commons.io.FileUtils;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.io.File;
//import java.io.IOException;
//import java.rmi.registry.LocateRegistry;
//import java.rmi.registry.Registry;
//import java.util.*;
//import java.util.List;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//
///**
// * @author caijy
// * @description
// * @date 2024/3/4 星期一 5:44 下午
// */
//public class EnterMainClassDialog extends DialogWrapper {
//
//    private Project project;
//
//    private Consumer<SelectCallBackModel> callback;
//
//    private List<MainClassInfoModel> mainClasses;
//
//    private List<String> classNames;
//
//    // 查找class输入框
//    private TextFieldWithAutoCompletion classNamesCompletion;
//
//    private boolean validateClassName;
//
//    // 查找启动类输入框
//    private TextFieldWithAutoCompletion textFieldWithAutoCompletion;
//
//    private boolean validateMainClass;
//
//    private CompletionProvider completionProvider;
//
//    private JPanel jPanel = new JPanel(new GridLayout(3, 1));
//
//    public EnterMainClassDialog(Project project, Consumer<SelectCallBackModel> callback) {
//        super(project);
//        this.project = project;
//        this.callback = callback;
//        this.mainClasses = Lists.newArrayList();
//        this.classNames = Lists.newArrayList();
//        this.completionProvider = new CompletionProvider(this.compare(project), IconLoader.findIcon("./icons/show.svg"));
//        setSize(800, 50);
//        setTitle("RuntimeClass Configuration");
//        setResizable(true);
//        init();
//    }
//
//    private List<MatchedVmModel> compare(Project project) {
//        // 扫描 mainClass
//        List<MainClassInfoModel> mainClasses = scan(project);
//        // 查找进程
//        List<VirtualMachineDescriptor> list = VirtualMachine.list();
//        // 匹配
//        return Optional.ofNullable(mainClasses).orElse(Collections.emptyList()).stream().map(mainClass -> {
//            MatchedVmModel vmModel = new MatchedVmModel();
//            vmModel.setMainClass(mainClass.getClassName());
//            vmModel.setRunning(0);
//            vmModel.setModuleName(mainClass.getModuleName());
//            VirtualMachineDescriptor matchedVm = list.stream().filter(vm -> vm.displayName().equals(mainClass.getClassName())).findFirst().orElse(null);
//            if (matchedVm != null) {
//                vmModel.setRunning(1);
//                vmModel.setPid(matchedVm.id());
//            }
//            return vmModel;
//        }).filter(vm -> vm.getRunning() == 1).collect(Collectors.toList());
//    }
//
//    private List<MainClassInfoModel> scan(Project project) {
//        ModuleManager moduleManager = ModuleManager.getInstance(project);
//        Module[] modules = moduleManager.getModules();
//        PsiManager psiManager = PsiManager.getInstance(project);
//        for (Module module : modules) {
//            if (!module.getName().equals(project.getName())) {
//                VirtualFile file = module.getModuleFile();
//                scanFile(file.getParent(), psiManager, module);
//            }
//        }
//        return mainClasses;
//    }
//
//    private void scanFile(VirtualFile file, PsiManager psiManager, Module module) {
//        if (!file.isDirectory() && file.getName().endsWith(".java")) {
//            String fileBody = null;
//            try {
//                fileBody = FileUtils.readFileToString(new File(file.getPath()), "UTF-8");
//            } catch (IOException e) {
//
//            }
//            if (fileBody == null) {
//                return;
//            }
//            PsiJavaFile javaFile = (PsiJavaFile) psiManager.findFile(file);
//            String className = javaFile.getPackageName() + "." + javaFile.getName().replace(".java", "");
//            if (fileBody.contains("public static void main")) {
//                MainClassInfoModel infoModel = new MainClassInfoModel();
//                infoModel.setClassName(className);
//                infoModel.setModuleName(module.getName());
//                mainClasses.add(infoModel);
//            }
//            classNames.add(className);
//            return;
//        }
//
//        VirtualFile[] files = file.getChildren();
//        for (VirtualFile virtualFile : files) {
//            scanFile(virtualFile, psiManager, module);
//        }
//    }
//
//    @Override
//    public @Nullable JComponent createCenterPanel() {
//        // 查找class输入框
//        classNamesCompletion = new TextFieldWithAutoCompletion(this.project, new TextFieldWithAutoCompletion.StringsCompletionProvider(classNames, IconLoader.findIcon("./icons/show.svg")), true, null);
//        classNamesCompletion.addDocumentListener(new DocumentListener() {
//            @Override
//            public void beforeDocumentChange(@NotNull DocumentEvent event) {
//                System.out.println("classNamesCompletion.beforeDocumentChange: " + event.getDocument().getText());
//            }
//
//            @Override
//            public void documentChanged(@NotNull DocumentEvent event) {
//                String showName = event.getDocument().getText();
//                System.out.println("classNamesCompletion.documentChanged: " + showName);
//                validateClassName = classNames.contains(showName);
//            }
//        });
//        classNamesCompletion.setPlaceholder("please enter target class package like xx.xx.xx");
//        jPanel.add(classNamesCompletion, BorderLayout.NORTH);
//
//        // 查找启动类输入框
//        textFieldWithAutoCompletion = new TextFieldWithAutoCompletion(this.project, completionProvider, true, null);
//        textFieldWithAutoCompletion.addDocumentListener(new DocumentListener() {
//            @Override
//            public void beforeDocumentChange(@NotNull DocumentEvent event) {
//                System.out.println("textFieldWithAutoCompletion.beforeDocumentChange: " + event.getDocument().getText());
//            }
//
//            @Override
//            public void documentChanged(@NotNull DocumentEvent event) {
//                String showName = event.getDocument().getText();
//                System.out.println("textFieldWithAutoCompletion.documentChanged: " + showName);
//                validateMainClass = completionProvider.getShowNameVmMap().containsKey(showName);
//                check();
//            }
//        });
//        textFieldWithAutoCompletion.addFocusListener(new FocusAdapter() {
//            @Override
//            public void focusLost(FocusEvent e) {
//                System.out.println("textFieldWithAutoCompletion.focusLost");
//                check();
//            }
//        });
//        textFieldWithAutoCompletion.setPlaceholder("please enter your main class package like xx.xx.xx");
//        jPanel.add(textFieldWithAutoCompletion, BorderLayout.NORTH);
//
//
//        return jPanel;
//    }
//
//    @Override
//    protected void doOKAction() {
//        System.out.println("okAction");
//
//        setSize(1000, 2000);
//        repaintUI();
//
//        String classShowName = classNamesCompletion.getText();
//        String mainClassShowName = textFieldWithAutoCompletion.getText();
//        MatchedVmModel model = completionProvider.getShowNameVmMap().get(mainClassShowName);
//        if (model != null) {
//            // 回调
//            SelectCallBackModel callBackModel = new SelectCallBackModel();
//            callBackModel.setVmModel(model);
//            callBackModel.setTargetClassName(classShowName);
//            callback.accept(callBackModel);
//        }
//    }
//
//    private static void start(FileTree fileTree) {
//        try {
//            RemoteService remoteService = new RemoteAppService(fileTree);
//            // 导出远程对象并绑定到 RMI Registry
//            Registry registry = LocateRegistry.createRegistry(1099);
//            registry.rebind("RemoteService", remoteService);
//            System.out.println("RMI Server is running.");
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("RMI Server is running error.");
//        }
//    }
//
//    private void repaintUI() {
//        jPanel.removeAll();
//        jPanel.setLayout(new BorderLayout());
//
//        JPanel topPane = new JPanel(new GridLayout(1, 3));
//        List<String> showNames = Lists.newArrayList();
//        TextFieldWithAutoCompletion searchField = new TextFieldWithAutoCompletion(this.project, new TextFieldWithAutoCompletion.StringsCompletionProvider(showNames, IconLoader.findIcon("./icons/show.svg")), true, null);
//        searchField.addDocumentListener(new DocumentListener() {
//            @Override
//            public void documentChanged(@NotNull DocumentEvent event) {
//                String showName = event.getDocument().getText();
//                System.out.println("searchField.documentChanged: " + showName);
//            }
//        });
//        searchField.setPreferredSize(new Dimension(250, 30));
//        JPanel topRightPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        topRightPane.add(new JLabel(IconLoader.findIcon("./icons/show.svg")));
//        topRightPane.add(searchField);
//        topPane.add(topRightPane);
//        topPane.add(new JPanel());
//        topPane.add(new JPanel());
//
//        // 创建文件树
//        EditorFactory editorFactory = EditorFactory.getInstance();
//        DocumentImpl document = new DocumentImpl("");
//        EditorEx editor = (EditorEx) editorFactory.createEditor(document, project);
//        JScrollPane jScrollPane = new JScrollPane(editor.getComponent());
//        FileTree fileTree = new FileTree(project, editor, newShowName -> {
//            showNames.add(newShowName);
//            searchField.setVariants(showNames);
//        },null);
//        JPanel treeJPanel = new JPanel(new BorderLayout());
//
//        // 创建刷新按钮区域
//        JPanel leftTopPane = new JPanel(new BorderLayout());
//        leftTopPane.add(searchField, BorderLayout.WEST);
//
////        new GotItTooltip("got.it.id", "刷新按钮", this.project).
////                // 为了方便调试，设置为100，该提示会出现 100 次
////                        withShowCount(100).
////                // 引导提示内容
////                        withHeader("输入文本，点击翻译按钮即可完成翻译").
////                // 引导提示位置设置在翻译按钮的正下方位置
////                        show(autoRefreshCheckBox, GotItTooltip.BOTTOM_MIDDLE);
//
//        treeJPanel.add(leftTopPane, BorderLayout.NORTH);
//        treeJPanel.add(fileTree, BorderLayout.CENTER);
//        JScrollPane treeScrollPane = new JScrollPane(treeJPanel);
//        treeScrollPane.setColumnHeaderView(leftTopPane);
//
//        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, jScrollPane);
//        splitPane.setResizeWeight(0.3);
//
//        this.jPanel.add(topPane, BorderLayout.NORTH);
//        this.jPanel.add(splitPane, BorderLayout.CENTER);
//        this.jPanel.revalidate();
//        this.jPanel.repaint();
//
//        start(fileTree);
//    }
//
//    private void check() {
//        if (!validateClassName) {
//            ValidationInfo info = new ValidationInfo("请输入 class", classNamesCompletion);
//            List<ValidationInfo> infos = Lists.newArrayList(info);
//            updateErrorInfo(infos);
//            return;
//        }
//
//        if (!validateMainClass) {
//            ValidationInfo info = new ValidationInfo("请输入 main class", textFieldWithAutoCompletion);
//            List<ValidationInfo> infos = Lists.newArrayList(info);
//            updateErrorInfo(infos);
//            return;
//        }
//
//        setOKActionEnabled(true);
//        updateErrorInfo(Collections.emptyList());
//    }
//}
