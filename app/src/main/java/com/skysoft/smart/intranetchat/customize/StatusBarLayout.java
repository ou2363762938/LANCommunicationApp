package com.skysoft.smart.intranetchat.customize;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.skysoft.smart.intranetchat.R;


public class StatusBarLayout extends View {
    public static int sRootLayoutHeight = -1;
    public StatusBarLayout(Context context) {
        super(context);
        init(context);
    }

    public StatusBarLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatusBarLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public StatusBarLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        setBackground(context.getDrawable(R.drawable.custom_gradient_main_title));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (sRootLayoutHeight > 0) {
            setMeasuredDimension(widthMeasureSpec,sRootLayoutHeight);
        }else {
            setMeasuredDimension(widthMeasureSpec,heightMeasureSpec);
        }
    }

    @Override
    public Drawable getBackground() {
        return super.getBackground();
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
    }
}
