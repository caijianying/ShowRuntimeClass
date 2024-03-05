package com.xiaobaicai.plugin.dialog;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

/**
 * @author caijy
 * @description TODO
 * @date 2024/3/5 星期二 8:49 下午
 */
public class CompletionProvider extends TextFieldWithAutoCompletionListProvider<String> implements DumbAware {
    protected CompletionProvider(@Nullable Collection<String> variants) {
        super(variants);
    }

    @Override
    protected @NotNull String getLookupString(@NotNull String item) {
        return item;
    }

    @Override
    protected @Nullable Icon getIcon(@NotNull String item) {
        if (item == null){
            return null;
        }
        return IconLoader.findIcon("./icons/show.svg");
    }
}
