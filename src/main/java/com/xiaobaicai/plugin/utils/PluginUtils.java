package com.xiaobaicai.plugin.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiJavaFile;
import com.intellij.ui.components.JBScrollPane;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.xiaobaicai.plugin.constants.Constant;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @author caijy
 * @description
 * @date 2024/3/4 星期一 5:06 下午
 */
public class PluginUtils {

    private static final IdeaPluginDescriptor IDEA_PLUGIN_DESCRIPTOR;

    private static String agentJarPath;

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
//            path = "/Users/jianyingcai/Library/Application Support/JetBrains/IntelliJIdea2021.1/plugins/plugin-agent-20240328-all.jar";
//            path = "/Users/jianyingcai/IdeaProjects/ShowRuntimeClass/build/idea-sandbox/plugins/ShowRuntimeClass/lib/plugin-agent-20240328-all.jar";
            MessageUtil.info("agent jar: " + path);
        } catch (Throwable ex) {
            System.out.println("cannot find agent jar !");
            return;
        }
        saveInfoLog("getPathOK");
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(infoDTO.getPid());
            saveInfoLog("VirtualMachine.attach ok");
            vm.loadAgent(path, JSONUtil.toJsonStr(infoDTO));
            saveInfoLog("VirtualMachine.loadAgent ok");
        } catch (AgentLoadException e) {
            System.out.println("无影响");
        } catch (Throwable e) {
            saveErrorLog(exceptionToString(e));
            MessageUtil.infoOpenToolWindow("loadAgent出现异常! ");
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (IOException e) {
                    saveErrorLog(exceptionToString(e));
                }
            }
        }
    }

    public static String getAgentCoreJarPath() {
        if (agentJarPath == null) {
            agentJarPath = getJarPath(Constant.AGENT_PREFIX, Constant.AGENT_SUFFIX);
        }
        return agentJarPath;
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
//                if (StrUtil.contains(pathStr, StrUtil.SPACE)) {
//                    return StrUtil.builder().append(quotes).append(pathStr).append(quotes).toString();
//                }
                return pathStr;
            }
        }
        return StrUtil.EMPTY;
    }

    public static String exceptionToString(Throwable throwable) {
        // 使用StringWriter捕获异常信息
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();

        // 返回捕获的异常信息字符串
        return stringWriter.toString();
    }

    public static void saveInfoLog(String message) {
        String infoLogPath = "/Users/jianyingcai/IdeaProjects/ShowRuntimeClass/info.log";
        FileUtil.appendString(message + "\n", infoLogPath, StandardCharsets.UTF_8);
    }

    public static void saveErrorLog(Throwable ex) {
        String message = exceptionToString(ex);
        saveErrorLog(message);
    }

    public static void saveErrorLog(String message) {
        String errorLogPath = "/Users/jianyingcai/IdeaProjects/ShowRuntimeClass/error.log";
        FileUtil.appendString(message + "\n", errorLogPath, StandardCharsets.UTF_8);
    }
}
