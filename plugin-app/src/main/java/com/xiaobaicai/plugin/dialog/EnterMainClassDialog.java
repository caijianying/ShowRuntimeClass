package com.xiaobaicai.plugin.dialog;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.xiaobaicai.plugin.model.MainClassInfoModel;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import com.xiaobaicai.plugin.model.SelectVmModel;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.*;
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

    private Consumer<String> pidCallback;

    private List<MatchedVmModel> matchedVms;

    private EnterMainClassDialog dialog;

    public EnterMainClassDialog(Project project, Consumer<String> pidCallback) {
        super(project);
        this.project = project;
        this.pidCallback = pidCallback;
        this.matchedVms = compare(project);
        this.dialog = this;
        setSize(800, 10);
        setTitle("Input Main Class");
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
        }).collect(Collectors.toList());
    }

    private List<MainClassInfoModel> scan(Project project) {
        List<MainClassInfoModel> mainClasses = Lists.newArrayList();
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        PsiManager psiManager = PsiManager.getInstance(project);
        for (Module module : modules) {
            if (!module.getName().equals(project.getName())) {
                VirtualFile file = module.getModuleFile();
                scanFile(file.getParent(), mainClasses, psiManager, module);
            }
        }
        return mainClasses;
    }

    private void scanFile(VirtualFile file, List<MainClassInfoModel> mainClasses, PsiManager psiManager, Module module) {
        if (!file.isDirectory() && file.getName().endsWith(".java")) {
            String fileBody = null;
            try {
                fileBody = FileUtils.readFileToString(new File(file.getPath()), "UTF-8");
            } catch (IOException e) {

            }
            if (fileBody == null) {
                return;
            }
            if (fileBody.contains("public static void main")) {
                PsiJavaFile javaFile = (PsiJavaFile) psiManager.findFile(file);
                String className = javaFile.getPackageName() + "." + javaFile.getName().replace(".java", "");

                MainClassInfoModel infoModel = new MainClassInfoModel();
                infoModel.setClassName(className);
                infoModel.setModuleName(module.getName());
                mainClasses.add(infoModel);
            }
            return;
        }

        VirtualFile[] files = file.getChildren();
        for (VirtualFile virtualFile : files) {
            scanFile(virtualFile, mainClasses, psiManager, module);
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel jPanel = new JPanel(new BorderLayout());
        SelectVmModel vmModel = getVmProcessList();
        List<String> classNames = vmModel.getShowNames();

        TextFieldWithAutoCompletion textFieldWithAutoCompletion = new TextFieldWithAutoCompletion(this.project, new TextFieldWithAutoCompletion.StringsCompletionProvider(classNames, IconLoader.findIcon("./icons/show.svg")), true, null);
        textFieldWithAutoCompletion.addDocumentListener(new DocumentListener() {
            @Override
            public void beforeDocumentChange(@NotNull DocumentEvent event) {
                System.out.println("beforeDocumentChange: " + event.getDocument().getText());
            }

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                System.out.println("documentChanged: " + event.getDocument().getText());
                if (classNames.contains(event.getDocument().getText())) {
                    String pid = vmModel.getShowNamePidMap().get(event.getDocument().getText());
                    if (pid != null) {
                        // 回调
                        pidCallback.accept(pid);
                    }

                    // 关闭弹窗
                    dialog.close(CLOSE_EXIT_CODE);
                }
            }
        });
        textFieldWithAutoCompletion.setPlaceholder("please enter your main class package like xx.xx.xx");
        jPanel.add(textFieldWithAutoCompletion, BorderLayout.NORTH);
        return jPanel;
    }

    private SelectVmModel getVmProcessList() {
        SelectVmModel vmModel = new SelectVmModel();
        Map<String, String> showNamePidMap = new HashMap<>();
        List<String> showNames = this.matchedVms.stream().sorted(Comparator.comparingInt(MatchedVmModel::getRunning).reversed()).map(vm -> {
            if (vm.getRunning() == 1) {
                String showName = String.format("【%s】%s 【running】【pid=%s】", vm.getModuleName(), vm.getMainClass(), vm.getPid());
                showNamePidMap.put(showName, vm.getPid());
                return showName;
            }
            return String.format("【%s】%s", vm.getModuleName(), vm.getMainClass());
        }).collect(Collectors.toList());

        vmModel.setShowNames(showNames);
        vmModel.setShowNamePidMap(showNamePidMap);
        return vmModel;
    }

    @Override
    protected boolean setAutoAdjustable(boolean autoAdjustable) {
        return false;
    }

    @Override
    protected @NotNull JPanel createButtonsPanel(@NotNull List<? extends JButton> buttons) {
        List<JButton> emptyButtons = new ArrayList<>();
        return super.createButtonsPanel(emptyButtons);
    }
}
