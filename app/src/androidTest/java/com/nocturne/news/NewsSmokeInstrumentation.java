package com.nocturne.news;

import android.app.Instrumentation;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextPaint;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import java.time.Instant;
import java.util.List;

/** Runs actual background JobScheduler + Google RSS HTTPS, never opens an Activity. */
public final class NewsSmokeInstrumentation extends Instrumentation {
    private boolean physical,readerChecks;
    @Override public void onCreate(Bundle args) { super.onCreate(args); physical=args!=null && "true".equals(args.getString("physical")); readerChecks=args!=null && "true".equals(args.getString("reader")); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            Context context=getTargetContext();NewsStore store=new NewsStore(context);
            long before=System.currentTimeMillis();
            NewsJobs.ensurePeriodic(context);NewsJobs.requestNow(context);
            JobInfo periodic=context.getSystemService(JobScheduler.class).getPendingJob(NewsJobs.PERIODIC);
            check(periodic!=null && periodic.isPersisted() && periodic.getIntervalMillis()==900_000,"Persisted 15-minute job");
            while(store.fetchedAt()<before && System.currentTimeMillis()-before<50_000) Thread.sleep(250);
            check(store.fetchedAt()>=before,"Real Google RSS background fetch completed");
            List<NewsItem> items=store.items();check(items.size()>1,"Multiple real headlines cached");
            LongTextComplicationData first=NewsComplicationService.buildData(context,store,0);
            LongTextComplicationData second=NewsComplicationService.buildData(context,store,300_000);
            String a=first.getText().getTextAt(context.getResources(),Instant.now()).toString();
            String b=second.getText().getTextAt(context.getResources(),Instant.now()).toString();
            check(!a.equals(b),"Different headlines supplied to native complication");
            TextPaint paint=new TextPaint();paint.setTextSize(13f);
            check(paint.measureText(a)<=225.1f && paint.measureText(b)<=225.1f,"Excerpts fit pill");
            check(first.getTapAction()!=null && first.getMonochromaticImage()!=null,"Native icon and tap action");
            ShortTextComplicationData shortData=(ShortTextComplicationData)NewsComplicationService.forType(first,ComplicationType.SHORT_TEXT);
            check(a.equals(shortData.getText().getTextAt(context.getResources(),Instant.now()).toString()),"WatchMaker short layout receives complete fitted excerpt");
            RangedValueComplicationData ticker=NewsComplicationService.buildTickerData(context,store,0);
            String full=ticker.getText().getTextAt(context.getResources(),Instant.now()).toString();
            check(full.equals(items.get(0).displayTitle()),"Ticker receives the full headline without excerpt truncation");
            TickerText measured=TickerText.prepare(context,full);
            check(ticker.getValue()==measured.width && ticker.getValue()>=217 && ticker.getValue()<=TickerText.MAX_WIDTH,"Ticker carries exact font width");
            String nextTicker=NewsComplicationService.buildTickerData(context,store,300_000).getText().getTextAt(context.getResources(),Instant.now()).toString();
            check(!full.equals(nextTicker),"Ticker changes to next headline");
            check(ticker.getTapAction()!=null && ticker.getMonochromaticImage()!=null,"Ticker retains native tap and icon");
            if(!physical) {
                long fetched=store.fetchedAt();store.failedFetch();
                check(store.fetchedAt()==fetched && store.items().size()==items.size(),"Failed fetch preserves cached news");
                String cached=NewsComplicationService.buildData(context,store,0).getTitle().getTextAt(context.getResources(),Instant.now()).toString();
                check(cached.contains("CACHED"),"Offline cache labeled");
                store.save(items);
            }
            String ui=readerChecks?NewsReaderChecks.run(this):"Reader UI checks not requested.";
            result.putString("stream","PASS: actual Google RSS fetched via background JobScheduler without opening an Activity; "+items.size()+" headlines; persisted periodic job; changing SHORT_TEXT/LONG_TEXT excerpts; full ticker headlines; exact 17-point font width; ticker rotation/icon/tap. "+ui+" "+(physical?"Physical device provider verified; on-face animation requires visual check.":"Emulator cache failure state verified; physical face NOT tested.")+"\n");
            finish(-1,result);
        } catch(Throwable failure) {
            result.putString("stream","FAIL: "+failure.getClass().getSimpleName()+": "+failure.getMessage()+"\n");finish(0,result);
        }
    }
    private void check(boolean condition,String name) { if(!condition)throw new AssertionError(name); }
}
