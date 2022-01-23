package com.linuxgods.kreiger.idea.statusbar.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class IndexWidgetFactory extends StatusBarEditorBasedWidgetFactory {
    @Override public @NonNls @NotNull String getId() {
        return "Index";
    }

    @Override public @Nls @NotNull String getDisplayName() {
        return "String Line:Column && Index";
    }

    @Override public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new IndexWidget(project);
    }

    @Override public void disposeWidget(@NotNull StatusBarWidget widget) {
        Disposer.dispose(widget);
    }
}
