package com.xiaobaicai.plugin.utils;

import com.intellij.ide.lightEdit.LightEditService;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.xiaobaicai.plugin.constants.Constant;
import org.jetbrains.annotations.NotNull;

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
        Notification notification = new Notification("Print", "小提示", message, NotificationType.INFORMATION);
        notification.addAction(new NotificationAction("查看错误堆栈") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(logPath);
                LightEditService.getInstance().openFile(virtualFile);
            }
        });
        Notifications.Bus.notify(notification, null);
    }
}
