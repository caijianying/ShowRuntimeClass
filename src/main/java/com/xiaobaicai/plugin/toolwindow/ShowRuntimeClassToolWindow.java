package com.xiaobaicai.plugin.toolwindow;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.GotItMessage;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;
import com.xiaobaicai.plugin.core.model.RemoteResponse;
import com.xiaobaicai.plugin.core.service.RemoteService;
import com.xiaobaicai.plugin.core.utils.RemoteUtil;
import com.xiaobaicai.plugin.model.MatchedVmReturnModel;
import com.xiaobaicai.plugin.utils.PluginUtils;
import com.xiaobaicai.plugin.utils.ProjectCache;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.Set;

/**
 * @author caijy
 * @description
 * @date 2024/3/18 星期一 2:00 上午
 */
public class ShowRuntimeClassToolWindow implements ToolWindowFactory {
    @SneakyThrows
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 中心内容
        ContentFactory contentFactory = ContentFactory.getInstance();
        ShowRuntimeClassPage page = new ShowRuntimeClassPage(project, vmModel -> {
            AttachVmInfoDTO vmInfoDTO = new AttachVmInfoDTO();
            vmInfoDTO.setPid(vmModel.getPid());
            vmInfoDTO.setPort(ProjectCache.getInstance().getMainClassPort(vmModel.getMainClass()));
            Set<String> availableClasses = null;
            try {
                PluginUtils.attach(vmInfoDTO);
                RemoteResponse<RemoteService> response = RemoteUtil.getRemoteService(vmInfoDTO.getPort());
                if (!response.isSuccess()) {
                    PluginUtils.handleError(response.getMessage());
                    return null;
                }
                RemoteService remoteService = response.getData();
                String classStr = remoteService.getAllAvailableClasses();
                availableClasses = Sets.newHashSet(classStr.split(","));
            } catch (Exception e) {
                PluginUtils.handleError(e);
            }

            MatchedVmReturnModel returnModel = new MatchedVmReturnModel();
            returnModel.setClasses(availableClasses);
            returnModel.setPort(vmInfoDTO.getPort());
            return returnModel;
        });
        Content content = contentFactory.createContent(page.createUIComponents(), "", false);
        // 将被界面工厂代理后创建的content，添加到工具栏窗口管理器中
        toolWindow.getContentManager().addContent(content);


        String step1Message="1. 输入当前项目下的启动类，插件会自动为你匹配到运行的进程！";
        String step2Message="2. 查找该进程中运行时的class";
        Icon usageIcon = IconLoader.findIcon("/icons/usage.svg");
        TextFieldWithAutoCompletion mainClassAutoCompletion = page.mainClassAutoCompletion;
        TextFieldWithAutoCompletion classNamesCompletion = page.classNamesCompletion;
        AnAction usageAction = new AnAction(usageIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                GotItMessage.createMessage("新手引导", step1Message)
                        .setCallback(()->{
                            GotItMessage.createMessage("新手引导", step2Message)
                                    .show(new RelativePoint(classNamesCompletion, new Point(0, classNamesCompletion.getHeight() / 2)), Balloon.Position.atLeft);
                        })
                        .show(new RelativePoint(mainClassAutoCompletion, new Point(0, mainClassAutoCompletion.getHeight() / 2)), Balloon.Position.atLeft);
            }
        };
        usageAction.getTemplatePresentation().setText("Usage");

        toolWindow.setTitleActions(Lists.newArrayList(usageAction));
    }
}
