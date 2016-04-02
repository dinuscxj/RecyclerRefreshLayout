package app.dinus.com.example.model;

import android.text.TextUtils;

public class ItemModel implements CursorModel{
    public static final String MORE_CURSOR = "more_cursor";

    private String mCursor;

    private String mTitle;
    private String mContent;

    public ItemModel(String mTitle, String mContent) {
        this.mCursor = MORE_CURSOR;

        this.mTitle = mTitle;
        this.mContent = mContent;
    }

    public ItemModel(String mTitle, String mContent, String mCursor) {
        this.mCursor = mCursor;

        this.mTitle = mTitle;
        this.mContent = mContent;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getContent() {
        return mContent;
    }

    @Override
    public boolean hasMore() {
        return !TextUtils.isEmpty(mCursor);
    }
}
