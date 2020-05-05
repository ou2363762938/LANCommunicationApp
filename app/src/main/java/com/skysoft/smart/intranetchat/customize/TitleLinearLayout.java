package com.skysoft.smart.intranetchat.customize;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.skysoft.smart.intranetchat.R;

public class TitleLinearLayout extends ConstraintLayout {
    public static int sLayoutHeight;
    public TitleLinearLayout(Context context) {
        super(context);
        init(context);
    }

    public TitleLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TitleLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackground(context.getDrawable(R.drawable.custom_gradient_main_title));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec,sLayoutHeight);
    }
}
