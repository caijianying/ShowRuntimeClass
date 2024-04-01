package com.xiaobaicai.plugin.toolwindow;

import cn.hutool.core.net.NetUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.GotItTooltip;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;
import com.xiaobaicai.plugin.core.service.RemoteService;
import com.xiaobaicai.plugin.core.utils.RemoteUtil;
import com.xiaobaicai.plugin.model.MatchedVmReturnModel;
import com.xiaobaicai.plugin.utils.MessageUtil;
import com.xiaobaicai.plugin.utils.PluginUtils;
import com.xiaobaicai.plugin.utils.ProjectCache;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        ShowRuntimeClassPage page = new ShowRuntimeClassPage(project, vmModel -> {
            AttachVmInfoDTO vmInfoDTO = new AttachVmInfoDTO();
            vmInfoDTO.setPid(vmModel.getPid());
            vmInfoDTO.setPort(NetUtil.getUsableLocalPort());
            Set<String> availableClasses = null;
            try {
                PluginUtils.attach(vmInfoDTO);
                RemoteService remoteService = RemoteUtil.getRemoteService(vmInfoDTO.getPort());
                if (remoteService == null) {
                    MessageUtil.infoOpenToolWindow("远程服务出现问题!");
                    return null;
                }
                String classStr = remoteService.getAllAvailableClasses();
                availableClasses = Sets.newHashSet(classStr.split(","));
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 保存app->port的映射
            ProjectCache.getInstance().mainClassPortMap.put(vmModel.getMainClass(), vmInfoDTO.getPort());
            MatchedVmReturnModel returnModel = new MatchedVmReturnModel();
            returnModel.setClasses(availableClasses);
            returnModel.setPort(vmInfoDTO.getPort());
            return returnModel;
        });
        Content content = contentFactory.createContent(page.createUIComponents(), "", false);
        // 将被界面工厂代理后创建的content，添加到工具栏窗口管理器中
        toolWindow.getContentManager().addContent(content);


        // 在标题栏的工具栏中添加一个自定义动作
        Icon questionIcon = IconLoader.findIcon("./icons/question.svg");
        AnAction customAction = new AnAction(questionIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 在这里可以处理动作被触发时的逻辑
                // 例如，弹出一个菜单或者执行其他操作
                System.out.println("Custom action triggered!");
            }
        };

        // 设置工具提示
        String message = "ok.I am fine thank you and you?";
        customAction.getTemplatePresentation().setText(message);

        GotItTooltip step1 = new GotItTooltip("got.it.id", "1. 输入当前项目下的启动类，插件会自动为你匹配到运行的进程！", project).
                // 为了方便调试，设置为100，该提示会出现 100 次
                        withShowCount(100);
        GotItTooltip step2 = new GotItTooltip("got.it.id", "2. 查找该进程中运行时的class", project).
                // 为了方便调试，设置为100，该提示会出现 100 次
                        withShowCount(100);
        AnAction usageAction = new AnAction(IconLoader.findIcon("./icons/usage.svg")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                step1.show(page.mainClassAutoCompletion, GotItTooltip.BOTTOM_MIDDLE);
                step2.show(page.classNamesCompletion, GotItTooltip.BOTTOM_MIDDLE);
            }
        };

        AnAction faqAction = new AnAction(IconLoader.findIcon("./icons/faq.svg")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 在这里可以处理动作被触发时的逻辑
                // 例如，弹出一个菜单或者执行其他操作
                System.out.println("faqAction triggered!");
            }
        };

        toolWindow.setTitleActions(Lists.newArrayList(customAction, usageAction, faqAction));

    }
}
