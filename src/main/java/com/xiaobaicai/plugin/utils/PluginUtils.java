package com.xiaobaicai.plugin.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import com.xiaobaicai.plugin.constants.Constant;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * @author caijy
 * @description
 * @date 2024/3/4 星期一 5:06 下午
 */
public class PluginUtils {

    private static final IdeaPluginDescriptor IDEA_PLUGIN_DESCRIPTOR;

    private static String agentJarPath;

    private static String logDir;

    static {
        PluginId pluginId = PluginId.getId(Constant.PLUGIN_ID);
        IDEA_PLUGIN_DESCRIPTOR = PluginManagerCore.getPlugin(pluginId);
    }


    public static void attach(AttachVmInfoDTO infoDTO) {
        String path;
        try {
            path = getAgentCoreJarPath();
        } catch (Throwable ex) {
            handleError(ex);
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
            String logPath = saveErrorLog(exceptionToString(e));
            MessageUtil.infoOpenLogFile("插件错误", logPath);
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (IOException e) {
                    String logPath = saveErrorLog(exceptionToString(e));
                    MessageUtil.infoOpenLogFile("插件错误", logPath);
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
        Path pluginPath = IDEA_PLUGIN_DESCRIPTOR.getPluginPath();
        if (pluginPath != null && pluginPath.toFile() != null) {
            List<File> files = FileUtil.loopFiles(pluginPath.toFile());
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith(startWith) && name.endsWith(suffix)) {
                    String pathStr = FileUtil.getCanonicalPath(file);
                    return pathStr;
                }
            }
        }
        MessageUtil.warn("agentLib not exist!");
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
        String infoLogPath = getLogDir() + File.separator + "info.log";
        FileUtil.appendString(message + "\n", infoLogPath, StandardCharsets.UTF_8);
    }

    public static String saveErrorLog(Throwable ex) {
        String message = exceptionToString(ex);
        return saveErrorLog(message);
    }

    public static String saveErrorLog(String message) {
        String errorLogPath = getLogDir() + File.separator + "error.log";
        FileUtil.appendString(message + "\n", errorLogPath, StandardCharsets.UTF_8);
        return errorLogPath;
    }

    private static String getLogDir() {
        if (logDir != null) {
            return logDir;
        }
        try {
            File parentFile = new File(getAgentCoreJarPath()).getParentFile().getParentFile();
            logDir = parentFile.getAbsolutePath();
            return logDir;
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void handleError(Throwable ex) {
        String path = saveErrorLog(ex);
        MessageUtil.infoOpenLogFile("插件错误！", path);
    }

    public static void handleError(String errorMsg) {
        String logPath = saveErrorLog(errorMsg);
        MessageUtil.infoOpenLogFile("插件错误！", logPath);
    }

}