package com.xiaobaicai.plugin.toolwindow;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;

import javax.swing.*;
import java.awt.*;

/**
 * @author caijy
 * @description
 * @date 2024/7/10 星期三 19:49
 */
public class ToolbarPanel extends JPanel {
    public ToolbarPanel(JComponent contentComponent, ActionGroup actions) {
        this(contentComponent, actions, "unknown");
    }

    public ToolbarPanel(JComponent contentComponent, ActionGroup actions, String toolbarPlace) {
        super(new GridBagLayout());
        this.setBorder(BorderFactory.createEtchedBorder());
        if (contentComponent.getBorder() != null) {
            contentComponent.setBorder(BorderFactory.createEmptyBorder());
        }

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(toolbarPlace, actions, true);
        this.add(actionToolbar.getComponent(), new GridBagConstraints(0, -1, 1, 1, 1.0D, 0.0D, 17, 2, new Insets(0, 0, 0, 0), 0, 0));
        this.add(contentComponent, new GridBagConstraints(0, -1, 1, 1, 1.0D, 1.0D, 17, 1, new Insets(0, 0, 0, 0), 0, 0));
    }
}
