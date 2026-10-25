package com.nocturne.news;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

/** The numeric value is a pixel width, not news freshness or a progress gauge. */
public final class TickerText {
    public static final int VIEWPORT_WIDTH=217;
    public static final int MAX_WIDTH=8192;
    public static final float FONT_SIZE=17f;
    public final String text;
    public final float width;
    private TickerText(String text,float width) { this.text=text;this.width=width; }
    public static TickerText prepare(Context context,String fullText) {
        TextPaint paint=new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.createFromAsset(context.getAssets(),"Roboto-Regular.ttf"));
        paint.setTextSize(FONT_SIZE);
        String text=NewsItem.normalize(fullText);
        // Only an extreme malformed title is bounded; ordinary headlines remain complete.
        if(paint.measureText(text)>MAX_WIDTH-4)
            text=TextUtils.ellipsize(text,paint,MAX_WIDTH-4,TextUtils.TruncateAt.END).toString();
        float width=Math.max(VIEWPORT_WIDTH,(float)Math.ceil(paint.measureText(text)+2));
        return new TickerText(text,width);
    }
}
