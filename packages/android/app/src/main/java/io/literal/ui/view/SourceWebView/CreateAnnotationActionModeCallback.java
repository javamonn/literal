package io.literal.ui.view.SourceWebView;
import android.graphics.Rect;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import io.literal.R;
import kotlin.jvm.functions.Function1;

public class CreateAnnotationActionModeCallback extends ActionMode.Callback2 {
    private final ActionMode.Callback2 originalCallback;
    private final int menu;
    private final Function1<ActionMode, Void> onAnnotationCreated;
    private final Function1<ActionMode, Void> onAnnotationCommitEdit;
    private final Function1<ActionMode, Void> onAnnotationCancelEdit;

    public CreateAnnotationActionModeCallback(
            ActionMode.Callback2 originalCallback,
            int menu,
            Function1<ActionMode, Void> onAnnotationCreated,
            Function1<ActionMode, Void> onAnnotationCommitEdit,
            Function1<ActionMode, Void> onAnnotationCancelEdit
    ) {
        this.originalCallback = originalCallback;
        this.menu = menu;
        this.onAnnotationCreated = onAnnotationCreated;
        this.onAnnotationCommitEdit = onAnnotationCommitEdit;
        this.onAnnotationCancelEdit = onAnnotationCancelEdit;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(this.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_annotate:
                if (onAnnotationCreated != null) {
                    onAnnotationCreated.invoke( mode);
                }

                // FIXME: call this after getting the selection from the webview
                mode.finish();
                return true;
            case R.id.menu_item_commit_edit:
                if (onAnnotationCommitEdit != null) {
                    onAnnotationCommitEdit.invoke(mode);
                }
                mode.finish();
                return true;
            case R.id.menu_item_cancel_edit:
                if (onAnnotationCancelEdit != null) {
                    onAnnotationCancelEdit.invoke(mode);
                }
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.originalCallback.onDestroyActionMode(mode);
    }

    @Override
    public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
        originalCallback.onGetContentRect(mode, view, outRect);
    }

    public static class Builder {
        private ActionMode.Callback2 originalCallback;
        private int menu;
        private Function1<ActionMode, Void> onAnnotationCreated;
        private Function1<ActionMode, Void> onAnnotationCommitEdit;
        private Function1<ActionMode, Void> onAnnotationCancelEdit;

        public Builder() {

        }

        public Builder setOnAnnotationCreated(Function1<ActionMode, Void> onAnnotationCreated) {
            this.onAnnotationCreated = onAnnotationCreated;
            return this;
        }

        public Builder setOnAnnotationCommitEdit(Function1<ActionMode, Void> onAnnotationCommitEdit) {
            this.onAnnotationCommitEdit = onAnnotationCommitEdit;
            return this;
        }

        public Builder setOnAnnotationCancelEdit(Function1<ActionMode, Void> onAnnotationCancelEdit) {
            this.onAnnotationCancelEdit = onAnnotationCancelEdit;
            return this;
        }

        public Builder setMenu(int menu) {
            this.menu = menu;
            return this;
        }

        public Builder setOriginalCallback(ActionMode.Callback2 originalCallback) {
            this.originalCallback = originalCallback;
            return this;
        }

        public CreateAnnotationActionModeCallback build() {
            return new CreateAnnotationActionModeCallback(
                    originalCallback,
                    menu,
                    onAnnotationCreated,
                    onAnnotationCommitEdit,
                    onAnnotationCancelEdit
            );
        }
    }
}
