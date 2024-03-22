package com.xiaobaicai.plugin.app.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author caijy
 * @description
 * @date 2024/3/1 星期五 5:47 下午
 */
public class ShowRuntimeClassAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
//        PsiJavaFile psiJavaFile = (PsiJavaFile) actionEvent.getDataContext().getData("psi.File");
//        Module module = (Module) actionEvent.getDataContext().getData("module");
//        String name = module.getName();
//        Project project = actionEvent.getProject();
    }
}
