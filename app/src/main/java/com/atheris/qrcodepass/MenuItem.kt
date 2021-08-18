package com.atheris.qrcodepass

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.widget.FrameLayout
import android.widget.TextView

class MenuItem(mContext : Context, attrs: AttributeSet) : FrameLayout(mContext,attrs) {
    init {
        inflate(context, R.layout.menu_item, this)

        findViewById<TextView>(R.id.menu_item_text).let {
            it.text = if (isInEditMode) "content of menu item" else {
                context.theme.obtainStyledAttributes(attrs, R.styleable.MenuItem, 0, 0)
                    .getString(R.styleable.MenuItem_text)
            }
        }

    }
    private fun dpToPx(dp:Int):Float {
        val displayMetrics = this.resources.displayMetrics;
        return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}