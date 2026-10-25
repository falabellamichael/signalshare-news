package com.nocturne.news;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextPaint;
import android.text.TextUtils;
import androidx.wear.watchface.complications.data.*;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class NewsComplicationService extends ComplicationDataSourceService {
    @Override public void onComplicationActivated(int id,ComplicationType type) {
        new NewsStore(this).active(id,true);
        NewsJobs.ensurePeriodic(this); NewsJobs.requestNow(this);
    }
    @Override public void onComplicationDeactivated(int id) {
        NewsStore store=new NewsStore(this); store.active(id,false);
        if(!store.hasActive()) NewsJobs.stopPeriodic(this);
    }
    @Override public void onComplicationRequest(ComplicationRequest request,ComplicationRequestListener listener) {
        NewsStore store=new NewsStore(this);
        store.active(request.getComplicationInstanceId(),true);
        NewsJobs.ensurePeriodic(this);
        if(System.currentTimeMillis()-store.fetchedAt()>NewsJobs.PERIOD
                && System.currentTimeMillis()-store.attemptedAt()>300_000) NewsJobs.requestNow(this);
        long now=System.currentTimeMillis();
        ComplicationData data=request.getComplicationType()==ComplicationType.RANGED_VALUE
                ? buildTickerData(this,store,now) : forType(buildData(this,store,now),request.getComplicationType());
        try { listener.onComplicationData(data); }
        catch(RemoteException disconnected) { /* Watchface disconnected. */ }
    }
    @Override public ComplicationData getPreviewData(ComplicationType type) {
        if(type==ComplicationType.RANGED_VALUE) return ticker(this,"Headline ticker preview","Google News ticker preview");
        if(type!=ComplicationType.LONG_TEXT && type!=ComplicationType.SHORT_TEXT) return null;
        return forType(build(this,"GOOGLE NEWS","Background headlines","Preview of Google News headlines"),type);
    }
    /** Ranged payload carries measured text width for the native ticker; legacy text slots stay unchanged. */
    public static RangedValueComplicationData buildTickerData(Context context,NewsStore store,long now) {
        List<NewsItem> items=store.items();
        if(items.isEmpty()) {
            String text=store.failed()?"Waiting for internet":"Fetching latest headlines…";
            return ticker(context,text,"Google News. "+text);
        }
        NewsItem item=items.get(NewsItem.indexAt(now,items.size()));
        boolean cached=store.failed() || now-store.fetchedAt()>3_600_000;
        return ticker(context,item.displayTitle(),item.displayTitle()+". "+item.source+(cached?". Cached headlines.":"."),item.link);
    }
    private static RangedValueComplicationData ticker(Context context,String fullText,String description) {
        return ticker(context,fullText,description,"");
    }
    private static RangedValueComplicationData ticker(Context context,String fullText,String description,String link) {
        TickerText prepared=TickerText.prepare(context,fullText);
        return new RangedValueComplicationData.Builder(prepared.width,0,TickerText.MAX_WIDTH,plain(description))
                .setText(plain(prepared.text))
                .setMonochromaticImage(new MonochromaticImage.Builder(Icon.createWithResource(context,R.drawable.ic_news)).build())
                .setTapAction(bulletinTap(context,link)).build();
    }
    public static ComplicationData forType(LongTextComplicationData data,ComplicationType type) {
        if(type==ComplicationType.SHORT_TEXT) return new ShortTextComplicationData.Builder(data.getText(),data.getContentDescription())
                .setTitle(data.getTitle()).setMonochromaticImage(data.getMonochromaticImage()).setTapAction(data.getTapAction()).build();
        return type==ComplicationType.LONG_TEXT ? data : null;
    }
    public static LongTextComplicationData buildData(Context context,NewsStore store,long now) {
        List<NewsItem> items=store.items();
        if(items.isEmpty()) return build(context,"GOOGLE NEWS",store.failed()?"Waiting for internet":"Fetching latest headlines…","Google News. Waiting for headlines.");
        NewsItem item=items.get(NewsItem.indexAt(now,items.size()));
        String time=DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(store.fetchedAt()));
        boolean cached=store.failed() || now-store.fetchedAt()>3_600_000;
        String header=(cached?"NEWS · CACHED ":"GOOGLE NEWS · ")+time;
        return build(context,header,fit(item.displayTitle()),item.displayTitle()+". "+item.source+". "+(cached?"Cached. ":"")+"Retrieved "+time,item.link);
    }
    private static LongTextComplicationData build(Context context,String title,String text,String description) {
        return build(context,title,text,description,"");
    }
    private static LongTextComplicationData build(Context context,String title,String text,String description,String link) {
        return new LongTextComplicationData.Builder(plain(text),plain(description)).setTitle(plain(title))
                .setMonochromaticImage(new MonochromaticImage.Builder(Icon.createWithResource(context,R.drawable.ic_news)).build())
                .setTapAction(bulletinTap(context,link)).build();
    }
    private static PendingIntent bulletinTap(Context context,String link) {
        Intent intent=new Intent(context,MainActivity.class);
        // A distinct Intent identity keeps cached headline taps from opening a different story.
        if(!link.isEmpty()) intent.setData(new Uri.Builder().scheme("nocturne").authority("bulletin").appendPath(link).build()).putExtra(MainActivity.BULLETIN_LINK,link);
        return PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    public static String fit(String text) {
        TextPaint paint=new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL)); paint.setTextSize(13f);
        return TextUtils.ellipsize(NewsItem.normalize(text),paint,225f,TextUtils.TruncateAt.END).toString();
    }
    private static PlainComplicationText plain(String value) { return new PlainComplicationText.Builder(value).build(); }
}
