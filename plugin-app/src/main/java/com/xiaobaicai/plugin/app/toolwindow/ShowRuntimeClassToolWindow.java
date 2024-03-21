package com.xiaobaicai.plugin.app.toolwindow;

import cn.hutool.core.net.NetUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.xiaobaicai.plugin.app.model.MatchedVmReturnModel;
import com.xiaobaicai.plugin.app.utils.PluginUtils;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;
import com.xiaobaicai.plugin.core.service.RemoteService;
import com.xiaobaicai.plugin.core.utils.RemoteUtil;
import org.jetbrains.annotations.NotNull;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author caijy
 * @description
 * @date 2024/3/18 星期一 2:00 上午
 */
public class ShowRuntimeClassToolWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // ContentFactory 在 IntelliJ 平台 SDK 中负责UI界面的管理
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        ShowRuntimeClassPage dialog = new ShowRuntimeClassPage(project, vmModel -> {
            AttachVmInfoDTO vmInfoDTO = new AttachVmInfoDTO();
            vmInfoDTO.setPid(vmModel.getPid());
//            vmInfoDTO.setPort(NetUtil.getUsableLocalPort());
            vmInfoDTO.setPort(1099);
            PluginUtils.attach(vmInfoDTO);

            RemoteService remoteService = RemoteUtil.getRemoteService(vmInfoDTO.getPort());
            Set<String> availableClasses = null;
            try {
                String classStr = remoteService.getAllAvailableClasses();
                availableClasses = Sets.newHashSet(classStr.split(","));
            } catch (RemoteException e) {
                e.printStackTrace();
            }

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
