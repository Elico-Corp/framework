package com.suez.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.view.View;

import com.odoo.R;


/**
 * Created by joseph on 18-4-28.
 */

public class ClearEditText extends android.support.v7.widget.AppCompatEditText implements View.OnFocusChangeListener, TextWatcher {
    private Drawable mClearDrawable;
    private boolean hasFocus;

    private boolean showIcon = true;

    private ITextLengthChangeListener textLengthChangeListener;

    public ClearEditText(Context context){
        this(context, null);
    }

    public ClearEditText(Context context, AttributeSet attrs){
        this(context, attrs, R.attr.editTextStyle);
    }

    public ClearEditText(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        mClearDrawable = getCompoundDrawables()[2];
        if (mClearDrawable == null){
            mClearDrawable = getResources().getDrawable(R.drawable.delete_selector);
        }

        mClearDrawable.setBounds(0, 0, mClearDrawable.getIntrinsicWidth(), mClearDrawable.getIntrinsicHeight());
        setClearIconVisible(false);
        setOnFocusChangeListener(this);
        addTextChangedListener(this);
    }

    protected void setClearIconVisible(boolean visible){
        Drawable right = showIcon && visible ? mClearDrawable : null;
        setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1], right, getCompoundDrawables()[3]);
    }

    public interface ITextLengthChangeListener{
        public void onTextLength(int length);
    }

    public static Animation shakeAnimation(int count){
        Animation translateAnimation = new TranslateAnimation(0, 10, 0, 0);
        translateAnimation.setDuration(80);
        translateAnimation.setRepeatCount(count);
        translateAnimation.setRepeatMode(Animation.REVERSE);
        return translateAnimation;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_UP){
            if (getCompoundDrawables()[2] != null){
                boolean touchable = event.getX() > (getWidth() - getTotalPaddingRight()) && (event.getX() < (getWidth() - getPaddingRight()));
                if (touchable){
                    this.setText("");
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus){
        this.hasFocus = hasFocus;
        if (hasFocus){
            setClearIconVisible(getText().length()>0);
        }else {
            setClearIconVisible(false);
        }
    }

    public void setFocusable(boolean focusable){
        this.hasFocus = focusable;
        if (focusable){
            setClearIconVisible(getText().length()>0);
        }else {
            setClearIconVisible(false);
        }
    }

    public void setShowIcon(boolean showIcon){
        this.showIcon = showIcon;
        setClearIconVisible(hasFocus);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int count, int after){
        if (hasFocus){
            setClearIconVisible(s.length()>0);
            if (textLengthChangeListener != null){
                textLengthChangeListener.onTextLength(s.length());
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after){}

    @Override
    public void afterTextChanged(Editable s){}

    public void setShakeAnimation(){
        this.startAnimation(shakeAnimation(5));
    }

    public void setTextLengthChangeListener(ITextLengthChangeListener listener){
        this.textLengthChangeListener = listener;
    }
}
