package com.jlocation;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

/**
 * A simple divider decoration with customizable colour, height, and left and right padding.
 */

public class NormDividerDecoration extends RecyclerView.ItemDecoration {
    private int mItemDividerHeight;
    private int mCategoryDividerHeight;
    private int mLPadding;
    private int mRPadding;
    private boolean mIncludeHeader = false;
    private boolean mIncludeFooter = false;
    private boolean mHoriPadding = false;

    private Paint mPaint;

    private NormDividerDecoration(int itemDividerHeight, int lPadding, int rPadding, int categoryDividerHeight, int colour, boolean includeHeader, boolean includeFooter, boolean horiPadding) {
        mItemDividerHeight = itemDividerHeight;
        mLPadding = lPadding;
        mRPadding = rPadding;
        mCategoryDividerHeight = categoryDividerHeight;
        mPaint = new Paint();
        mPaint.setColor(colour);

        mIncludeHeader = includeHeader;
        mIncludeFooter = includeFooter;
        mHoriPadding = horiPadding;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int count = parent.getChildCount();
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = parent.getAdapter();
        int top, bottom, left, right;
        int prevType = adapter.getItemViewType(0);
        for (int i = 1; i < count; i++) {
            int curType = adapter.getItemViewType(i);
            final View child = parent.getChildAt(i - 1);

            if (curType == prevType) {
                top = child.getBottom();
                bottom = top + mItemDividerHeight;
                left = child.getLeft() + mLPadding;
                right = child.getRight() - mRPadding;

                c.save();
                c.drawRect(left, top, right, bottom, mPaint);
                c.restore();
            }

            prevType = curType;
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int curPosition = parent.getChildLayoutPosition(view);
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = parent.getAdapter();
        int curType = adapter.getItemViewType(curPosition);
        int itemNum = adapter.getItemCount();

        Integer prevItemType = null;
        Integer nextItemType = null;

        if (curPosition > 0) {
            prevItemType = adapter.getItemViewType(curPosition - 1);
        }

        if (curPosition + 1 < itemNum) {
            nextItemType = adapter.getItemViewType(curPosition + 1);
        }

        if (prevItemType == null) {
            if (mIncludeHeader) {
                outRect.top = mCategoryDividerHeight;
            }

            if (nextItemType == null) {
                if (mIncludeFooter) {
                    outRect.bottom = mCategoryDividerHeight;
                }
            }
        } else {
            if (curType == prevItemType) {
                outRect.top = mItemDividerHeight;
            } else {
                outRect.top = mCategoryDividerHeight;
            }
        }

        if (mHoriPadding) {
            outRect.left = mLPadding;
            outRect.right = mRPadding;
        }

    }

    /**
     * A basic builder for divider decorations. The default builder creates a 1px thick black divider decoration.
     */
    public static class Builder {
        private Resources mResources;
        private int mItemDividerHeight;
        private int mCategoryDividerHeight;
        private int mLPadding;
        private int mRPadding;
        private int mColour;

        public Builder(Context context) {
            mResources = context.getResources();
            mItemDividerHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 1f, context.getResources().getDisplayMetrics());
            mCategoryDividerHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 20f, context.getResources().getDisplayMetrics());
            mLPadding = 0;
            mRPadding = 0;
            mColour = Color.BLACK;
        }

        /**
         * Set the divider height in pixels
         *
         * @param pixels height in pixels
         * @return the current instance of the Builder
         */
        public Builder setItemDividerHeight(float pixels) {
            mItemDividerHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, pixels, mResources.getDisplayMetrics());
            return this;
        }

        /**
         * Set the divider height in dp
         *
         * @param resource height resource id
         * @return the current instance of the Builder
         */
        public Builder setItemDividerHeight(@DimenRes int resource) {
            mItemDividerHeight = mResources.getDimensionPixelSize(resource);
            return this;
        }


        public Builder setCategoryDividerHeight(float pixels) {
            mCategoryDividerHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, pixels, mResources.getDisplayMetrics());
            return this;
        }

        public Builder setCategoryDividerHeight(@DimenRes int resource) {
            mCategoryDividerHeight = mResources.getDimensionPixelSize(resource);
            return this;
        }

        /**
         * Sets both the left and right padding in pixels
         *
         * @param pixels padding in pixels
         * @return the current instance of the Builder
         */
        public Builder setHorizontalPadding(float pixels) {
            setLeftPadding(pixels);
            setRightPadding(pixels);

            return this;
        }

        /**
         * Sets the left and right padding in dp
         *
         * @param resource padding resource id
         * @return the current instance of the Builder
         */
        public Builder setHorizontalPadding(@DimenRes int resource) {
            setLeftPadding(resource);
            setRightPadding(resource);
            return this;
        }

        /**
         * Sets the left padding in pixels
         *
         * @param pixelPadding left padding in pixels
         * @return the current instance of the Builder
         */
        public Builder setLeftPadding(float pixelPadding) {
            mLPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, pixelPadding, mResources.getDisplayMetrics());

            return this;
        }

        /**
         * Sets the right padding in pixels
         *
         * @param pixelPadding right padding in pixels
         * @return the current instance of the Builder
         */
        public Builder setRightPadding(float pixelPadding) {
            mRPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, pixelPadding, mResources.getDisplayMetrics());

            return this;
        }

        /**
         * Sets the left padding in dp
         *
         * @param resource left padding resource id
         * @return the current instance of the Builder
         */
        public Builder setLeftPadding(@DimenRes int resource) {
            mLPadding = mResources.getDimensionPixelSize(resource);

            return this;
        }

        /**
         * Sets the right padding in dp
         *
         * @param resource right padding resource id
         * @return the current instance of the Builder
         */
        public Builder setRightPadding(@DimenRes int resource) {
            mRPadding = mResources.getDimensionPixelSize(resource);

            return this;
        }

        /**
         * Sets the divider colour
         *
         * @param resource the colour resource id
         * @return the current instance of the Builder
         */
        public Builder setColorResource(@ColorRes int resource) {
            setColor(mResources.getColor(resource));

            return this;
        }

        /**
         * Sets the divider colour
         *
         * @param color the colour
         * @return the current instance of the Builder
         */
        public Builder setColor(@ColorInt int color) {
            mColour = color;

            return this;
        }

        /**
         * Instantiates a DividerDecoration with the specified parameters.
         *
         * @return a properly initialized DividerDecoration instance
         */
        public NormDividerDecoration build(boolean includeHead, boolean includeFooter, boolean horiPadding) {
            return new NormDividerDecoration(mItemDividerHeight, mLPadding, mRPadding, mCategoryDividerHeight, mColour, includeHead, includeFooter, horiPadding);
        }
    }
}

