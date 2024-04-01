package com.xiaobaicai.plugin.dialog;

import com.intellij.openapi.project.DumbAware;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author caijy
 * @description
 * @date 2024/3/5 星期二 8:49 下午
 */
public class CompletionProvider extends TextFieldWithAutoCompletionListProvider<MatchedVmModel> implements DumbAware {

    private Map<String, MatchedVmModel> showNameVmMap = new HashMap<>();

    public CompletionProvider(Collection<MatchedVmModel> variants) {
        super(variants);
    }

    public Map<String, MatchedVmModel> getShowNameVmMap() {
        return showNameVmMap;
    }


    @Override
    protected @NotNull String getLookupString(@NotNull MatchedVmModel vm) {
        String showName = String.format("【%s】【%s】%s", vm.getPid(), vm.getModuleName(), vm.getMainClass());
        showNameVmMap.put(showName, vm);
        return showName;
    }

    @Override
    public int compare(MatchedVmModel item1, MatchedVmModel item2) {
        return item2.getRunning().compareTo(item1.getRunning());
    }

}
