package com.xiaobaicai.plugin.app.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import com.xiaobaicai.plugin.app.utils.PluginUtils;
import com.xiaobaicai.plugin.app.dialog.EnterMainClassDialog;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;
import org.jetbrains.annotations.NotNull;

/**
 * @author caijy
 * @description
 * @date 2024/3/1 星期五 5:47 下午
 */
public class ShowRuntimeClassAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        PsiJavaFile psiJavaFile = (PsiJavaFile) actionEvent.getDataContext().getData("psi.File");
        Module module = (Module) actionEvent.getDataContext().getData("module");
        String name = module.getName();
        Project project = actionEvent.getProject();
        EnterMainClassDialog dialog = new EnterMainClassDialog(project, callBackModel -> {
            AttachVmInfoDTO vmInfoDTO = new AttachVmInfoDTO();
            vmInfoDTO.setPid(callBackModel.getVmModel().getPid());
            vmInfoDTO.setTargetClassName(callBackModel.getTargetClassName());
            PluginUtils.attach(vmInfoDTO);
        });
        dialog.show();
    }
}
