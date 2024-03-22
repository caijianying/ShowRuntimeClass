package com.xiaobaicai.plugin.app.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author caijy
 * @description
 * @date 2024/3/21 星期四 3:24 下午
 */
@State(name = "ShowRuntimeClass", storages = {@Storage(value = "show-runtime-class.xml")})
public class ProjectCache implements PersistentStateComponent<ProjectCache> {

    public Map<String, Integer> mainClassPortMap = new HashMap<>();

    public static ProjectCache getInstance() {
        return ApplicationManager.getApplication().getService(ProjectCache.class);
    }

    @Override
    public @Nullable ProjectCache getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ProjectCache projectCache) {
        this.mainClassPortMap = projectCache.mainClassPortMap;
    }
}
