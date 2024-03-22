package com.xiaobaicai.plugin.app.toolwindow;

import cn.hutool.core.net.NetUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.xiaobaicai.plugin.app.model.MatchedVmReturnModel;
import com.xiaobaicai.plugin.app.utils.PluginUtils;
import com.xiaobaicai.plugin.app.utils.ProjectCache;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;
import com.xiaobaicai.plugin.core.service.RemoteService;
import com.xiaobaicai.plugin.core.utils.RemoteUtil;
import org.jetbrains.annotations.NotNull;

import java.rmi.RemoteException;
import java.util.Set;

/**
 * @author caijy
 * @description
 * @date 2024/3/18 星期一 2:00 上午
 */
public class ShowRuntimeClassToolWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 在标题栏的工具栏中添加一个自定义动作
        AnAction customAction = new AnAction(IconLoader.findIcon("./icons/question.svg")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 在这里可以处理动作被触发时的逻辑
                // 例如，弹出一个菜单或者执行其他操作
                System.out.println("Custom action triggered!");
            }
        };
        // 设置工具提示
        customAction.getTemplatePresentation().setText("ok.I am fine thank you and you?");
        toolWindow.setTitleActions(Lists.newArrayList(customAction));

        // 中心内容
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        ShowRuntimeClassPage dialog = new ShowRuntimeClassPage(project, vmModel -> {
            AttachVmInfoDTO vmInfoDTO = new AttachVmInfoDTO();
            vmInfoDTO.setPid(vmModel.getPid());
            vmInfoDTO.setPort(NetUtil.getUsableLocalPort());
            PluginUtils.attach(vmInfoDTO);

            RemoteService remoteService = RemoteUtil.getRemoteService(vmInfoDTO.getPort());
            Set<String> availableClasses = null;
            try {
                String classStr = remoteService.getAllAvailableClasses();
                availableClasses = Sets.newHashSet(classStr.split(","));
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // 保存app->port的映射
            ProjectCache.getInstance().mainClassPortMap.put(vmModel.getMainClass(), vmInfoDTO.getPort());

            MatchedVmReturnModel returnModel = new MatchedVmReturnModel();
            returnModel.setClasses(availableClasses);
            returnModel.setPort(vmInfoDTO.getPort());
            return returnModel;
        });
        Content content = contentFactory.createContent(dialog.createUIComponents(), "", false);
        // 将被界面工厂代理后创建的content，添加到工具栏窗口管理器中
        toolWindow.getContentManager().addContent(content);
    }
}
