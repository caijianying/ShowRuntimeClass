package com.xiaobaicai.plugin.dialog;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;

/**
 * @author caijy
 * @description
 * @date 2024/4/10 星期三 13:50
 */
public class EditorDialog extends DialogWrapper {

    private EditorEx editor;

    private Project project;

    private Document document;

    private VirtualFile virtualFile;

    public EditorDialog(VirtualFile virtualFile, Project project) {
        super(project);
        this.project = project;
        this.virtualFile = virtualFile;
        this.document = FileDocumentManager.getInstance().getDocument(virtualFile);
        setTitle("Editor Dialog");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        editor = (EditorEx) editorFactory.createEditor(this.document, this.project);
        EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        editor.setColorsScheme(colorsScheme);
        EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
        editor.setHighlighter(highlighterFactory.createEditorHighlighter(this.project, this.virtualFile));
        editor.setViewer(true);
        JBScrollPane scrollPane = new JBScrollPane(editor.getComponent());
        return scrollPane;
    }
}
