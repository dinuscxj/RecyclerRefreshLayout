package com.dinuscxj.example.model;

import android.text.TextUtils;

public class OpenProjectModel implements CursorModel{
    public static final String MORE_CURSOR = "more_cursor";

    private final String mCursor;

    private final String mTitle;
    private final String mContent;
    private final String mAuthor;
    private final String mColor;

    public OpenProjectModel(String mTitle, String mContent, String mAuthor, String mColor) {
        this.mCursor = MORE_CURSOR;

        this.mTitle = mTitle;
        this.mContent = mContent;
        this.mAuthor = mAuthor;
        this.mColor = mColor;
    }

    public OpenProjectModel(String mTitle, String mContent, String mColor) {
        this(mTitle, mContent, "dinus", mColor);
    }

    public String getTitle() {
        return mTitle;
    }

    public String getContent() {
        return mContent;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getColor() {
        return mColor;
    }

    @Override
    public boolean hasMore() {
        return !TextUtils.isEmpty(mCursor);
    }
}
