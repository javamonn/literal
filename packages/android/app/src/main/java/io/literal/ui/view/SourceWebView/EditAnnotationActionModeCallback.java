package io.literal.ui.view.SourceWebView;

import android.graphics.Rect;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.jetbrains.annotations.NotNull;

import io.literal.R;
import io.literal.lib.Callback;

public class EditAnnotationActionModeCallback extends ActionMode.Callback2 {
    Rect annotationBoundingBox;
    Callback<Void, Void> onEditAnnotation;
    Callback<Void, Void> onDeleteAnnotation;

    public EditAnnotationActionModeCallback(@NotNull Rect annotationBoundingBox, @NotNull Callback<Void, Void> onEditAnnotation, @NotNull Callback<Void, Void> onDeleteAnnotation) {
        this.annotationBoundingBox = annotationBoundingBox;
        this.onEditAnnotation = onEditAnnotation;
        this.onDeleteAnnotation = onDeleteAnnotation;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.source_webview_edit_annotation_menu, menu);
        return true;
    }

    public void setAnnotationBoundingBox(Rect annotationBoundingBox) {
        this.annotationBoundingBox = annotationBoundingBox;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_edit:
                onEditAnnotation.invoke(null, null);
                return true;
            case R.id.menu_item_remove:
                onDeleteAnnotation.invoke(null, null);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }

    @Override
    public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
        outRect.set(annotationBoundingBox);
    }
}
