package com.xiaobaicai.plugin.app.dialog;

import com.google.common.collect.Lists;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.xiaobaicai.plugin.app.model.MainClassInfoModel;
import com.xiaobaicai.plugin.app.model.MatchedVmModel;
import com.xiaobaicai.plugin.app.model.SelectCallBackModel;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author caijy
 * @description
 * @date 2024/3/4 星期一 5:44 下午
 */
public class EnterMainClassDialog extends DialogWrapper {

    private Project project;

    private Consumer<SelectCallBackModel> callback;

    private List<MainClassInfoModel> mainClasses;

    private List<String> classNames;

    // 查找class输入框
    private TextFieldWithAutoCompletion classNamesCompletion;

    private boolean validateClassName;

    // 查找启动类输入框
    private TextFieldWithAutoCompletion textFieldWithAutoCompletion;

    private boolean validateMainClass;

    private CompletionProvider completionProvider;

    public EnterMainClassDialog(Project project, Consumer<SelectCallBackModel> callback) {
        super(project);
        this.project = project;
        this.callback = callback;
        this.mainClasses = Lists.newArrayList();
        this.classNames = Lists.newArrayList();
        this.completionProvider = new CompletionProvider(this.compare(project), IconLoader.findIcon("./icons/show.svg"));
        setSize(800, 50);
        setTitle("RuntimeClass Configuration");
        setResizable(true);
        init();
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

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel jPanel = new JPanel(new GridLayout(2, 1));

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
                validateClassName = classNames.contains(showName);
            }
        });
        classNamesCompletion.setPlaceholder("please enter target class package like xx.xx.xx");
        jPanel.add(classNamesCompletion, BorderLayout.NORTH);

        // 查找启动类输入框
        textFieldWithAutoCompletion = new TextFieldWithAutoCompletion(this.project, completionProvider, true, null);
        textFieldWithAutoCompletion.addDocumentListener(new DocumentListener() {
            @Override
            public void beforeDocumentChange(@NotNull DocumentEvent event) {
                System.out.println("textFieldWithAutoCompletion.beforeDocumentChange: " + event.getDocument().getText());
            }

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                String showName = event.getDocument().getText();
                System.out.println("textFieldWithAutoCompletion.documentChanged: " + showName);
                validateMainClass = completionProvider.getShowNameVmMap().containsKey(showName);
                check();
            }
        });
        textFieldWithAutoCompletion.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                System.out.println("textFieldWithAutoCompletion.focusLost");
                check();
            }
        });
        textFieldWithAutoCompletion.setPlaceholder("please enter your main class package like xx.xx.xx");
        jPanel.add(textFieldWithAutoCompletion, BorderLayout.NORTH);
        return jPanel;
    }

    @Override
    protected void doOKAction() {
        System.out.println("okAction");
        String classShowName = classNamesCompletion.getText();
        String mainClassShowName = textFieldWithAutoCompletion.getText();
        MatchedVmModel model = completionProvider.getShowNameVmMap().get(mainClassShowName);
        if (model != null) {
            // 回调
            SelectCallBackModel callBackModel = new SelectCallBackModel();
            callBackModel.setVmModel(model);
            callBackModel.setTargetClassName(classShowName);
            callback.accept(callBackModel);
            this.close(DialogWrapper.OK_EXIT_CODE);
        }
    }

    private void check() {
        if (!validateClassName) {
            ValidationInfo info = new ValidationInfo("请输入 class", classNamesCompletion);
            List<ValidationInfo> infos = Lists.newArrayList(info);
            updateErrorInfo(infos);
            return;
        }

        if (!validateMainClass) {
            ValidationInfo info = new ValidationInfo("请输入 main class", textFieldWithAutoCompletion);
            List<ValidationInfo> infos = Lists.newArrayList(info);
            updateErrorInfo(infos);
            return;
        }

        setOKActionEnabled(true);
        updateErrorInfo(Collections.emptyList());
    }
}
