package com.xiaobaicai.plugin.listener;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.xiaobaicai.plugin.scan.FileScanner;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author caijy
 * @description
 * @date 2024/10/21 星期一 17:00
 */
public class PluginListener implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 在项目加载完成后执行的代码
        System.out.println("项目加载完成后执行的代码.execute");

        // 如果有耗时任务，使用后台线程执行
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Scan project and compare") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    FileScanner.INSTANCE.compare(project);
                });
            }
        });

        return null;
    }
}
