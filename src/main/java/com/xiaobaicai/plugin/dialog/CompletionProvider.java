package com.xiaobaicai.plugin.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import com.xiaobaicai.plugin.model.MatchedVmReturnModel;
import com.xiaobaicai.plugin.tree.FileTree;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public void handleMainClassChoosed(String showName, Function<MatchedVmModel, MatchedVmReturnModel> callback, FileTree fileTree) {
        boolean matched = this.getShowNameVmMap().containsKey(showName);
        if (matched) {
            MatchedVmModel vmModel = this.getShowNameVmMap().get(showName);
            Future<MatchedVmReturnModel> modelFuture = ApplicationManager.getApplication().executeOnPooledThread(new Callable<MatchedVmReturnModel>() {
                @Override
                public MatchedVmReturnModel call() {
                    return callback.apply(vmModel);
                }
            });
            fileTree.handleFutureVmProcessChoosed(modelFuture);
        }
    }

    public void handleMainClassChoosedForEditor(String showName, Consumer<MatchedVmModel> consumer) {
        boolean matched = this.getShowNameVmMap().containsKey(showName);
        if (matched) {
            MatchedVmModel vmModel = this.getShowNameVmMap().get(showName);
            ApplicationManager.getApplication().executeOnPooledThread(() -> consumer.accept(vmModel));
        }

    }

}
