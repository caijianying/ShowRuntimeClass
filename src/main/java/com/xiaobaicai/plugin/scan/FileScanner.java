package com.xiaobaicai.plugin.scan;

import com.google.common.collect.Lists;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiMethodUtil;
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


    public List<MatchedVmModel> compare(Project project) {
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
                if (hasMainMethod(javaFile)) {
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
        while (true) {
            if (ProjectManager.getInstance().getDefaultProject().isInitialized()) {
                break;
            }
        }

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
