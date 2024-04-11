package com.xiaobaicai.plugin.action;

import cn.hutool.core.collection.CollectionUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.MouseChecker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.awt.RelativePoint;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;
import com.xiaobaicai.plugin.dialog.CompletionProvider;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import com.xiaobaicai.plugin.scan.FileScanner;
import com.xiaobaicai.plugin.utils.MessageUtil;
import com.xiaobaicai.plugin.utils.PluginUtils;
import com.xiaobaicai.plugin.utils.ProjectCache;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;


/**
 * @author caijy
 * @description
 * @date 2024/3/1 星期五 5:47 下午
 */
public class ShowRuntimeClassAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        String targetClassName = "";
        if (virtualFile.getName().endsWith(".java")) {
            PsiFile file = PsiManager.getInstance(e.getProject()).findFile(virtualFile);
            if (file instanceof PsiJavaFile) {
                PsiJavaFile javaFile = (PsiJavaFile) file;
                String packageName = javaFile.getPackageName();
                System.out.println(packageName);
                targetClassName = packageName + "." + javaFile.getName().replace(".java", "");
            }
        }
        Project project = e.getProject();
        List<MatchedVmModel> modelList = FileScanner.INSTANCE.compare(project);

        CompletionProvider completionProvider = new CompletionProvider(modelList);
        // 查找启动类输入框
        TextFieldWithAutoCompletion mainClassAutoCompletion = new TextFieldWithAutoCompletion(e.getProject(), completionProvider, true, null);
        mainClassAutoCompletion.setBorder(null);
        mainClassAutoCompletion.setPreferredSize(new Dimension(250, 30));

        // 创建一个面板，用于放置输入框
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(mainClassAutoCompletion);

        // 创建一个弹窗
        JBPopup jbPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, mainClassAutoCompletion)
                .setCancelOnWindowDeactivation(true)
                .setRequestFocus(true)
                .setAdText("输入你的主启动类，实时查看当前Java类的class文件")
                .setResizable(false)
                .setFocusable(true)
                .setMovable(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelOnMouseOutCallback(new MouseChecker() {
                    @Override
                    public boolean check(MouseEvent mouseEvent) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            mainClassAutoCompletion.setShowPlaceholderWhenFocused(true);
                            mainClassAutoCompletion.setPlaceholder("Main Class");
                        });
                        return true;
                    }
                })
                .setCancelKeyEnabled(true)
                .setCancelCallback(() -> true)
                .createPopup();

        String finalTargetClassName = targetClassName;
        mainClassAutoCompletion.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                List<MatchedVmModel> diffList = FileScanner.INSTANCE.compare(project);
                if (CollectionUtil.isEmpty(diffList)) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        jbPopup.setAdText("当前无可选进程", 2);
                    });
                }
                String showName = event.getDocument().getText();
                completionProvider.handleMainClassChoosedForEditor(showName, m -> {
                    String mainClass = m.getMainClass();
                    Integer port = ProjectCache.getInstance().getMainClassPort(mainClass);

                    AttachVmInfoDTO infoDTO = new AttachVmInfoDTO();
                    infoDTO.setPort(port);
                    infoDTO.setPid(m.getPid());
                    PluginUtils.attach(infoDTO);
                    String filePath = PluginUtils.retransformClassRemotely(port, finalTargetClassName);
                    if (filePath == null) {
                        MessageUtil.multiLayeredInfo(project, finalTargetClassName, mainClass);
                    } else {
                        PluginUtils.showEditorDialog(filePath, project);
                    }
                });
            }
        });

        Component component = e.getInputEvent().getComponent();
        Point locationOnScreen = component.getLocationOnScreen();
        int x = locationOnScreen.x;
        int y = locationOnScreen.y;

        // 计算弹窗的位置，位于图标下方
        Point popupLocation = new Point(x - 200, y);
        jbPopup.show(new RelativePoint(popupLocation));
    }


}
