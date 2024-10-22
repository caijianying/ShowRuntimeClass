package com.xiaobaicai.plugin.scan;

import com.google.common.collect.Lists;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.xiaobaicai.plugin.model.ClassInfoModel;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import lombok.Getter;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author caijy
 * @description
 * @date 2024/4/3 星期三 13:26
 */
@Getter
public class FileScanner {

    public static FileScanner INSTANCE = new FileScanner();

    private List<ClassInfoModel> mainClasses = Lists.newArrayList();

    private List<ClassInfoModel> allClasses = Lists.newArrayList();

    private List<String> classNames = Lists.newArrayList();

    private List<String> checkMainClassKeywords = Lists.newArrayList("public", "static", "void", "main");

    public List<MatchedVmModel> compare(Project project) {
        // 扫描 mainClass
        if (mainClasses == null) {
            mainClasses = scan(project);
        }
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
        PsiManager psiManager = PsiManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        if (modules != null && modules.length == 1) {
            Module module = modules[0];
            String dirPath = ModuleUtil.getModuleDirPath(module);
            File file = new File(dirPath);
            this.scanModule(file.getParentFile(), psiManager, module);
        } else {
            for (Module module : modules) {
                if (!module.getName().equals(project.getName())) {
                    String dirPath = ModuleUtil.getModuleDirPath(module);
                    File file = new File(dirPath);
                    this.scanModule(file, psiManager, module);
                }
            }
        }
        return mainClasses;
    }

    private void scanModule(File rootProjectDir, PsiManager psiManager, Module module) {
        if (rootProjectDir == null) {
            return;
        }
        if (rootProjectDir.isDirectory()) {
            File[] files = rootProjectDir.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File file : files) {
                if (file == null) {
                    continue;
                }
                // 支持单体项目
                if ("src".equals(file.getName())) {
                    scanFile(file, psiManager, module);
                }
                // 支持maven多模块
                if (module.getName().equals(file.getName())) {
                    File[] childFiles = file.listFiles();
                    if (childFiles == null) {
                        continue;
                    }
                    for (File listFile : childFiles) {
                        if ("src".equals(listFile.getName())) {
                            scanFile(file, psiManager, module);
                        }
                    }
                }
            }
        }
    }

    private void scanFile(File file, PsiManager psiManager, Module module) {
        if (file == null) {
            return;
        }
        if (!file.isDirectory()) {
            if (file.getName().endsWith(".java")) {
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(file.getAbsolutePath());
                if (virtualFile == null) {
                    return;
                }
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile == null) {
                    return;
                }
                FileType fileType = psiFile.getFileType();
                if (fileType.getName().equals("JAVA")) {
                    String code = psiFile.getFileDocument().getText();
                    String classSimpleName = psiFile.getName().substring(0, psiFile.getName().lastIndexOf("."));
                    String packageName = getPackageNameFromClassCode(code);
                    String className = packageName + "." + classSimpleName;
                    ClassInfoModel infoModel = new ClassInfoModel();
                    infoModel.setClassName(className);
                    infoModel.setModuleName(module.getName());
                    if (this.isMainClass(code)) {
                        if (!classNames.contains(className)) {
                            mainClasses.add(infoModel);
                        }
                    }
                    classNames.add(className);
                    allClasses.add(infoModel);
                }
            }
            return;
        }

        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File targetFile : files) {
            scanFile(targetFile, psiManager, module);
        }
    }

    public String getPackageNameFromClassCode(String code) {

        for (String line : code.split("\n")) {
            if (line.startsWith("package")) {
                return line.replace("package", "").replace(";", "").trim();
            }
        }
        return null;
    }

    public boolean isMainClass(String code) {
        for (String line : code.split("\n")) {
            boolean mainClassCheck = true;
            for (String classKeyword : checkMainClassKeywords) {
                if (!line.contains(classKeyword)) {
                    mainClassCheck = false;
                    break;
                }
            }
            if (mainClassCheck) {
                return true;
            }
        }
        return false;
    }
}
