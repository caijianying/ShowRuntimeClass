package com.xiaobaicai.plugin.utils;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.sun.tools.attach.VirtualMachine;
import com.xiaobaicai.plugin.constants.Constant;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;
import com.xiaobaicai.plugin.core.enhance.EnhanceOptEnum;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import com.xiaobaicai.plugin.scan.FileScanner;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @Description 右下角的吐司提示
 * @Date 2020/8/28
 * @Created by fangla
 */
public class MessageUtil {

    public static void error(String message) {
        Notification notification = new Notification("Print", LanguageUtil.getLocalMessages(Constant.MSG_ERROR), message, NotificationType.ERROR);
        Notifications.Bus.notify(notification, null);
    }

    public static void warn(String message) {
        Notification notification = new Notification("Print", LanguageUtil.getLocalMessages(Constant.MSG_ERROR), message, NotificationType.WARNING);
        Notifications.Bus.notify(notification, null);
    }

    public static void infoOpenToolWindow(String message) {
        Notification notification = new Notification("Print", LanguageUtil.getLocalMessages(Constant.MSG_INFO), message, NotificationType.INFORMATION);
        // 为顶层通知添加 Action，触发 Action 会弹出一个新的通知
        notification.addAction(new NotificationAction(LanguageUtil.getLocalMessages(Constant.MSG_SETTINGS)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), "Tools.ChatGPT\" Coding");
            }
        });
        Notifications.Bus.notify(notification, null);
    }

    public static void info(String message) {
        Notification notification = new Notification("Print", LanguageUtil.getLocalMessages(Constant.MSG_INFO), message, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification, null);
    }

    public static void infoOpenLogFile(String message, String logPath) {
        Notification notification = new Notification("Print", "提示", message, NotificationType.INFORMATION);
        notification.addAction(new NotificationAction("查看错误堆栈") {
            @SneakyThrows
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(logPath);
                FileEditorManager.getInstance(e.getProject()).openFile(virtualFile, true);
            }
        });
        Notifications.Bus.notify(notification, null);
    }

    /**
     * 多层通知
     **/
    public static void multiLayeredInfo(Project project, String fullClassName, String mainClassName) {
        Notification notification = new Notification("Print", "提示", "当前Java类没有运行时class，可能有以下原因。", NotificationType.INFORMATION);
        // 为顶层通知添加 Action，触发 Action 会弹出一个新的通知
        notification.addAction(new NotificationAction("查看原因") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                String tips = "1. 确保当前Java类【%s】在你选择的进程【%s】中。";
                tips = String.format(tips, fullClassName, mainClassName);
                Notification notifycation2 = new Notification("Print", "查看原因", tips, NotificationType.INFORMATION);
                notifycation2.addAction(new NotificationAction("Next") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent ex, @NotNull Notification notification) {
                        String tips2 = String.format("2. 请确保当前Java类【%s】已被实例化。", fullClassName);
                        Notification notify = new Notification("Print", "查看原因", tips2, NotificationType.INFORMATION);
                        Notifications.Bus.notify(notify, ex.getProject());
                    }
                });
                Notifications.Bus.notify(notifycation2, e.getProject());
            }
        });
        Notifications.Bus.notify(notification, project);
    }
}
