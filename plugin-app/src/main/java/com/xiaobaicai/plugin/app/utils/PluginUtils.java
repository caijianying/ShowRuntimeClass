package com.xiaobaicai.plugin.app.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiJavaFile;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.xiaobaicai.plugin.app.constants.Constant;
import com.xiaobaicai.plugin.core.boot.AgentPkgPath;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @author caijy
 * @description
 * @date 2024/3/4 星期一 5:06 下午
 */
public class PluginUtils {

    private static final IdeaPluginDescriptor IDEA_PLUGIN_DESCRIPTOR;

    static {
        PluginId pluginId = PluginId.getId(Constant.PLUGIN_ID);
        IDEA_PLUGIN_DESCRIPTOR = PluginManagerCore.getPlugin(pluginId);
    }

    /**
     * @return mainClass path
     **/
    public static String findMainClass(PsiJavaFile currentJavaFile, Module currentModule) {
        return null;
    }

    public static void attach(AttachVmInfoDTO infoDTO) {
        String path;
        try {
            path = getAgentCoreJarPath();
        } catch (Throwable ex) {
            System.out.println("cannot find agent jar !");
            return;
        }
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(infoDTO.getPid());
            vm.loadAgent(path, JSONUtil.toJsonStr(infoDTO));
        } catch (AttachNotSupportedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AgentInitializationException e) {
            System.out.println("agent init error !");
        } catch (AgentLoadException e) {

        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getAgentCoreJarPath() {
        return getJarPath(Constant.AGENT_PREFIX, Constant.AGENT_SUFFIX);
    }

    /**
     * 根据jar包的前缀名称获路径
     *
     * @param startWith 前缀名称
     * @return String
     */
    private static String getJarPath(String startWith, String suffix) {
        final String quotes = "\"";
        if (Objects.nonNull(IDEA_PLUGIN_DESCRIPTOR.getPath())) {
            //MessageUtil.info("agentLib:" + IDEA_PLUGIN_DESCRIPTOR.getPath());
        } else {
            MessageUtil.warn("agentLib not exist!");
        }
        List<File> files = FileUtil.loopFiles(IDEA_PLUGIN_DESCRIPTOR.getPath());
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(startWith) && name.endsWith(suffix)) {
                String pathStr = FileUtil.getCanonicalPath(file);
                if (StrUtil.contains(pathStr, StrUtil.SPACE)) {
                    return StrUtil.builder().append(quotes).append(pathStr).append(quotes).toString();
                }
                return pathStr;
            }
        }
        return StrUtil.EMPTY;
    }
}
