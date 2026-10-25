package com.nocturne.news;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared high-contrast primitives; minimum 48dp interactive targets. */
final class ReaderUi {
    static final int WHITE=Color.WHITE, MUTED=Color.rgb(205,214,226), ACCENT=Color.rgb(144,222,255);
    static final int SURFACE=Color.rgb(18,22,28), BORDER=Color.rgb(66,75,89);
    private final Context context;
    ReaderUi(Context context) { this.context=context; }
    int dp(float n) { return Math.round(n*context.getResources().getDisplayMetrics().density); }
    TextView text(String value,float size,int color,boolean bold) {
        TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        t.setIncludeFontPadding(false);t.setLineSpacing(dp(3),1f);
        t.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        t.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return t;
    }
    LinearLayout column() { LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.VERTICAL);v.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return v; }
    void gap(LinearLayout parent,int height) { View spacer=new View(context);parent.addView(spacer,new LinearLayout.LayoutParams(1,dp(height))); }
    TextView action(String label,boolean primary,int id,View.OnClickListener click) {
        TextView t=text(label,13,primary?Color.BLACK:WHITE,true);t.setId(id);t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(48));t.setPadding(dp(10),dp(10),dp(10),dp(10));
        t.setBackground(ripple(primary?ACCENT:SURFACE,primary?ACCENT:BORDER));
        t.setFocusable(true);t.setClickable(true);t.setOnClickListener(click);return t;
    }
    RippleDrawable ripple(int fill,int border) {
        GradientDrawable shape=new GradientDrawable();shape.setColor(fill);shape.setCornerRadius(dp(16));shape.setStroke(dp(1),border);
        GradientDrawable mask=new GradientDrawable();mask.setColor(WHITE);mask.setCornerRadius(dp(16));
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(65,255,255,255)),shape,mask);
    }
    void divider(LinearLayout parent) { View line=new View(context);line.setBackgroundColor(BORDER);parent.addView(line,new LinearLayout.LayoutParams(-1,dp(1))); }
}
