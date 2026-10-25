package com.nocturne.news;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** End-to-end native UI actions against the real cached feed; restores user reader settings. */
final class NewsReaderChecks {
    static String run(Instrumentation test) throws Exception {
        Context context=test.getTargetContext();NewsStore news=new NewsStore(context);ReaderStore reader=new ReaderStore(context);
        List<NewsItem> items=news.items();check(items.size()>1,"Live feed exists");
        NewsItem first=items.get(0);boolean originallySaved=reader.isSaved(first);int originalSize=reader.textSize();
        MainActivity activity=(MainActivity)test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        test.waitForIdleSync();
        try {
            check(activity.findViewById(R.id.reader_latest)!=null,"Latest screen opens");
            test.runOnMainSync(() -> firstCard((ViewGroup)activity.getWindow().getDecorView()).performClick());test.waitForIdleSync();
            check(first.displayTitle().contentEquals(((TextView)activity.findViewById(R.id.reader_headline)).getText()),"Tapping bulletin opens full headline");
            check(activity.findViewById(R.id.reader_phone).isEnabled(),"Valid story has phone action");
            test.runOnMainSync(() -> activity.findViewById(R.id.reader_save).performClick());
            check(reader.isSaved(first)!=originallySaved,"Save action persists a snapshot");
            test.runOnMainSync(() -> activity.findViewById(R.id.reader_text_size).performClick());test.waitForIdleSync();
            check(reader.textSize()!=originalSize,"Reader size control persists");
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);test.waitForIdleSync();
            check(activity.findViewById(R.id.reader_latest)!=null,"System back returns to bulletin list");
            if(originallySaved) reader.toggle(first); // Keep the item saved for the saved-list flow.
            test.runOnMainSync(() -> activity.findViewById(R.id.reader_saved).performClick());test.waitForIdleSync();
            check(firstCard((ViewGroup)activity.getWindow().getDecorView())!=null,"Saved list contains snapshot");
            test.runOnMainSync(() -> activity.findViewById(R.id.reader_latest).performClick());test.waitForIdleSync();
            // Deliver real complication PendingIntents while app is foreground; distinct identities must keep their stories.
            NewsComplicationService.buildTickerData(context,news,0).getTapAction().send();SystemClock.sleep(500);test.waitForIdleSync();
            check(first.displayTitle().contentEquals(((TextView)activity.findViewById(R.id.reader_headline)).getText()),"Ticker tap opens exact first story");
            NewsComplicationService.buildTickerData(context,news,300_000).getTapAction().send();SystemClock.sleep(500);test.waitForIdleSync();
            check(items.get(1).displayTitle().contentEquals(((TextView)activity.findViewById(R.id.reader_headline)).getText()),"Next ticker tap opens distinct second story");
            NewsComplicationService.buildTickerData(context,news,0).getTapAction().send();SystemClock.sleep(500);test.waitForIdleSync();
            check(first.displayTitle().contentEquals(((TextView)activity.findViewById(R.id.reader_headline)).getText()),"Original PendingIntent still opens original story");
            List<String> coverage=MainActivity.coverage(first);check(!coverage.isEmpty(),"Real RSS related coverage is available");
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);test.waitForIdleSync();
            test.runOnMainSync(() -> ((ScrollView)activity.findViewById(R.id.reader_scroll)).scrollTo(0,150));
            int oldY=((ScrollView)activity.findViewById(R.id.reader_scroll)).getScrollY();
            test.runOnMainSync(() -> activity.findViewById(R.id.reader_refresh).performClick());test.waitForIdleSync();
            check(((ScrollView)activity.findViewById(R.id.reader_scroll)).getScrollY()==oldY,"Refresh does not immediately reset list position");
            return "Reader UI PASS: bulletin tap, save snapshot, saved list, larger text, system back, three exact ticker taps, real related coverage, refresh preserving scroll. Phone delivery not exercised.";
        } finally {
            if(reader.isSaved(first)!=originallySaved) reader.toggle(first);reader.textSize(originalSize);
            test.runOnMainSync(activity::finish);
        }
    }
    private static View firstCard(ViewGroup parent) {
        for(int i=0;i<parent.getChildCount();i++) {
            View child=parent.getChildAt(i);
            if(child.getContentDescription()!=null && child.getContentDescription().toString().startsWith("Open bulletin:")) return child;
            if(child instanceof ViewGroup) { View found=firstCard((ViewGroup)child);if(found!=null)return found; }
        }
        return null;
    }
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
