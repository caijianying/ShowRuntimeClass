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
import java.net.URI;
import java.net.URISyntaxException;
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
            MessageUtil.info("agent jar: " + path);
        } catch (Throwable ex) {
            System.out.println("cannot find agent jar !");
            return;
        }
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(infoDTO.getPid());
            URI uri = new URI(path);
            String encodedPath = uri.toASCIIString();
            vm.loadAgent(encodedPath, JSONUtil.toJsonStr(infoDTO));
        } catch (Exception e) {
            e.printStackTrace();
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
                if (StrUtil.contains(pathStr, StrUtil.SPACE)) {
                    return StrUtil.builder().append(quotes).append(pathStr).append(quotes).toString();
                }
                return pathStr;
            }
        }
        return StrUtil.EMPTY;
    }

    public static String getDumpClassPath(String pid) {
        File agentFile = new File(getAgentCoreJarPath());
        String dumpClassPath = agentFile.getParent() + "/dump" + File.separator + pid;
        System.out.println("dumpClassPath:" + dumpClassPath);
        return dumpClassPath;
    }

    public static Component replaceEditorWithLoading(EditorEx editor) {
        // 获取编辑器所在的 JScrollPane
        Component component = editor.getScrollPane();
        while (!(component instanceof JBScrollPane) && component != null) {
            component = component.getParent();
        }
        if (component instanceof JBScrollPane) {
            JLabel loadingLabel = new JLabel("Loading...");
            loadingLabel.setVisible(true);
            // 设置loading文本的颜色
            loadingLabel.setForeground(Color.BLUE);
            // 设置loading图标
            loadingLabel.setIcon(new ImageIcon("./icons/loading.gif"));
            JBScrollPane scrollPane = (JBScrollPane) component;
            // 移除编辑器
            scrollPane.setViewportView(null);
            // 将 JProgressBar 添加到 JScrollPane 中
            scrollPane.setViewportView(loadingLabel);
            return loadingLabel;
        } else {
            Messages.showMessageDialog("Cannot find JBScrollPane containing Editor", "Error", null);
        }
        return null;
    }

    public static Component replaceLoadingWithEditor(EditorEx editor) {
        JScrollPane scrollPane = editor.getScrollPane();
        // 移除标签
        scrollPane.setViewportView(null);
        // 将编辑器添加到 JScrollPane 中
        scrollPane.setViewportView(editor.getContentComponent());
        return scrollPane;
    }
}
