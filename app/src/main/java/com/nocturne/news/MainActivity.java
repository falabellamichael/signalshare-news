package com.nocturne.news;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.wear.remote.interactions.RemoteActivityHelper;
import androidx.wear.widget.SwipeDismissFrameLayout;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Native bulletin reader. Background jobs and the ticker remain independent. */
public final class MainActivity extends Activity {
    public static final String BULLETIN_LINK="bulletin_link";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService remoteExecutor=Executors.newSingleThreadExecutor();
    private final SharedPreferences.OnSharedPreferenceChangeListener cacheListener=(prefs,key) -> {
        if("fetched_at".equals(key) || "failed".equals(key)) handler.post(this::cacheChanged);
    };
    private ReaderUi ui;
    private NewsStore news;
    private ReaderStore reader;
    private ScrollView scroll;
    private LinearLayout content;
    private TextView status,refreshButton;
    private NewsItem selected;
    private List<NewsItem> visibleItems=new ArrayList<>();
    private boolean savedOnly,refreshing,resumed;
    private int homeScroll;
    private long refreshStarted;
    private final Runnable refreshTimeout=() -> {
        refreshing=false;
        if(status!=null) status.setText("Refresh queued · check your connection");
        if(refreshButton!=null) refreshButton.setText("Refresh headlines");
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);ui=new ReaderUi(this);news=new NewsStore(this);reader=new ReaderStore(this);
        getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
        if(state!=null) {
            savedOnly=state.getBoolean("saved_only");homeScroll=state.getInt("home_scroll");
            try { if(state.containsKey("selected")) selected=NewsStore.decode(new JSONObject(state.getString("selected"))); }
            catch(Exception ignored) { selected=null; }
        }
        if(selected==null) resolveBulletin(getIntent());
        if(selected==null) showHome(homeScroll);else showBulletin(selected,state==null?0:state.getInt("detail_scroll"));
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::goBack);
    }
    @Override public void onResume() {
        super.onResume();resumed=true;news.observe(cacheListener);
        if(news.fetchedAt()==0 || System.currentTimeMillis()-news.fetchedAt()>NewsJobs.PERIOD) requestRefresh();
        else updateStatus();
    }
    @Override public void onPause() { resumed=false;refreshing=false;news.stopObserving(cacheListener);handler.removeCallbacks(refreshTimeout);super.onPause(); }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null);remoteExecutor.shutdownNow();super.onDestroy(); }
    @Override public void onSaveInstanceState(Bundle state) {
        state.putBoolean("saved_only",savedOnly);state.putInt("home_scroll",selected==null?scroll.getScrollY():homeScroll);
        if(selected!=null) try { state.putString("selected",NewsStore.encode(selected).toString());state.putInt("detail_scroll",scroll.getScrollY()); } catch(Exception ignored) {}
        super.onSaveInstanceState(state);
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);setIntent(intent);resolveBulletin(intent);
        if(selected==null) showHome(homeScroll);else showBulletin(selected,0);
    }
    private void resolveBulletin(Intent intent) {
        selected=null;String link=intent.getStringExtra(BULLETIN_LINK);
        if(link==null) return;
        List<NewsItem> all=new ArrayList<>(news.items());all.addAll(reader.saved());
        for(NewsItem item:all) if(link.equals(item.link)) { selected=item;return; }
    }
    private void screen(int y) {
        status=null;refreshButton=null;
        SwipeDismissFrameLayout frame=new SwipeDismissFrameLayout(this);frame.setBackgroundColor(Color.BLACK);
        scroll=new ScrollView(this);scroll.setId(R.id.reader_scroll);scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(true);scroll.setScrollbarFadingEnabled(false);scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        content=ui.column();content.setPadding(ui.dp(18),ui.dp(20),ui.dp(18),ui.dp(64));
        scroll.addView(content);frame.addView(scroll);setContentView(frame);
        frame.addCallback(new SwipeDismissFrameLayout.Callback() {
            @Override public void onDismissed(SwipeDismissFrameLayout layout) { goBack(); }
        });
        scroll.setFocusableInTouchMode(true);scroll.requestFocus();
        ScrollView currentScroll=scroll;currentScroll.post(() -> currentScroll.scrollTo(0,y));
    }
    private void showHome(int y) {
        selected=null;screen(y);
        TextView masthead=ui.text(getString(R.string.app_name),13,ReaderUi.WHITE,true);masthead.setLetterSpacing(.02f);masthead.setGravity(Gravity.CENTER);content.addView(masthead);
        ui.gap(content,5);
        status=ui.text("",10,ReaderUi.MUTED,false);status.setId(R.id.reader_status);status.setGravity(Gravity.CENTER);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);content.addView(status);updateStatus();
        ui.gap(content,8);
        LinearLayout tabs=new LinearLayout(this);
        TextView latest=ui.action("Latest",!savedOnly,R.id.reader_latest,v -> { savedOnly=false;showHome(0); });
        TextView saved=ui.action("Saved",savedOnly,R.id.reader_saved,v -> { savedOnly=true;showHome(0); });
        latest.setSelected(!savedOnly);saved.setSelected(savedOnly);latest.setStateDescription(savedOnly?"":"Selected");saved.setStateDescription(savedOnly?"Selected":"");
        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,ui.dp(48),1);half.setMarginEnd(ui.dp(6));tabs.addView(latest,half);tabs.addView(saved,new LinearLayout.LayoutParams(0,ui.dp(48),1));content.addView(tabs);
        ui.gap(content,10);
        visibleItems=savedOnly?reader.saved():news.items();
        if(visibleItems.isEmpty()) {
            TextView empty=ui.text(savedOnly?"Your reading list":"Your next briefing is on its way",21,ReaderUi.WHITE,true);content.addView(empty);ui.gap(content,8);
            content.addView(ui.text(savedOnly?"Open a bulletin and tap Save to keep it here.":"Connect to the internet and refresh to get the latest headlines.",14,ReaderUi.MUTED,false));
        }
        for(int i=0;i<visibleItems.size();i++) addCard(visibleItems.get(i),i);
        ui.gap(content,12);
        refreshButton=ui.action(refreshing?"Refreshing…":"Refresh headlines",true,R.id.reader_refresh,v -> requestRefresh());content.addView(refreshButton);
        ui.gap(content,12);TextView credit=ui.text("Headlines via Google News\nCanada · English",11,ReaderUi.MUTED,false);credit.setGravity(Gravity.CENTER);content.addView(credit);
    }
    private void addCard(NewsItem item,int index) {
        LinearLayout card=ui.column();card.setPadding(ui.dp(14),ui.dp(14),ui.dp(14),ui.dp(14));card.setBackground(ui.ripple(ReaderUi.SURFACE,ReaderUi.BORDER));
        String source=item.source.isEmpty()?"BULLETIN":item.source;
        TextView title=ui.text(item.displayTitle(),15.5f,ReaderUi.WHITE,true);title.setMaxLines(3);title.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(title);ui.gap(card,10);
        card.addView(ui.text(source,11,ReaderUi.ACCENT,true));ui.gap(card,5);
        card.addView(ui.text(ReaderFormat.age(item.published,System.currentTimeMillis())+"   ›",11,ReaderUi.MUTED,false));
        card.setContentDescription("Open bulletin: "+item.displayTitle()+". "+source);card.setFocusable(true);card.setClickable(true);
        card.setOnClickListener(v -> { homeScroll=scroll.getScrollY();showBulletin(item,0); });
        content.addView(card);ui.gap(content,10);
    }
    private void showBulletin(NewsItem item,int y) {
        selected=item;screen(y);
        content.addView(ui.action("‹  Headlines",false,View.NO_ID,v -> goBack()));ui.gap(content,16);
        TextView source=ui.text(item.source.isEmpty()?"NEWS BULLETIN":item.source.toUpperCase(java.util.Locale.getDefault()),11,ReaderUi.ACCENT,true);source.setLetterSpacing(.05f);content.addView(source);ui.gap(content,9);
        TextView title=ui.text(item.displayTitle(),reader.textSize()+1,ReaderUi.WHITE,true);title.setId(R.id.reader_headline);title.setAccessibilityHeading(true);content.addView(title);ui.gap(content,12);
        content.addView(ui.text(ReaderFormat.age(item.published,System.currentTimeMillis()),12,ReaderUi.MUTED,false));ui.gap(content,16);ui.divider(content);ui.gap(content,16);
        boolean validLink=ReaderFormat.safeLink(item.link);
        TextView phone=ui.action("Read on phone  ↗",true,R.id.reader_phone,v -> openOnPhone(item));phone.setEnabled(validLink);phone.setAlpha(validLink?1:.5f);content.addView(phone);ui.gap(content,8);
        TextView save=ui.action(reader.isSaved(item)?"Saved · tap to remove":"Save bulletin",false,R.id.reader_save,v -> { reader.toggle(item);((TextView)v).setText(reader.isSaved(item)?"Saved · tap to remove":"Save bulletin"); });content.addView(save);
        ui.gap(content,8);
        status=ui.text("",12,ReaderUi.MUTED,false);status.setId(R.id.reader_status);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);content.addView(status);
        List<String> coverage=coverage(item);
        if(!coverage.isEmpty()) {
            ui.gap(content,14);content.addView(ui.text("MORE COVERAGE",11,ReaderUi.ACCENT,true));ui.gap(content,12);
            for(String headline:coverage) { content.addView(ui.text(headline,reader.textSize(),ReaderUi.WHITE,false));ui.gap(content,18); }
        }
        ui.gap(content,8);content.addView(ui.text("Full reporting is available from the publisher. Open the story on your phone to continue reading.",13,ReaderUi.MUTED,false));ui.gap(content,16);
        content.addView(ui.action(reader.textSize()==20?"Text size · Larger":"Text size · Standard",false,R.id.reader_text_size,v -> { int pos=scroll.getScrollY();reader.textSize(reader.textSize()==20?16:20);showBulletin(item,pos); }));
        if(validLink) {
            Intent browser=new Intent(Intent.ACTION_VIEW,Uri.parse(item.link)).addCategory(Intent.CATEGORY_BROWSABLE);
            if(browser.resolveActivity(getPackageManager())!=null) { ui.gap(content,8);content.addView(ui.action("Open watch browser  ↗",false,View.NO_ID,v -> startActivity(browser))); }
        }
        ui.gap(content,16);content.addView(ui.text("Published "+item.published+"\nHeadlines via Google News",10,ReaderUi.MUTED,false));
    }
    static List<String> coverage(NewsItem item) {
        List<String> result=new ArrayList<>();
        // Render no HTML or web content: use only link labels from the RSS description.
        Spanned description=Html.fromHtml(item.coverage,Html.FROM_HTML_MODE_COMPACT,null,null);
        for(URLSpan span:description.getSpans(0,description.length(),URLSpan.class)) {
            if(!ReaderFormat.safeLink(span.getURL())) continue;
            String title=NewsItem.normalize(description.subSequence(description.getSpanStart(span),description.getSpanEnd(span)).toString());
            if(title.isEmpty() || title.equals(item.displayTitle()) || title.toLowerCase(java.util.Locale.ROOT).contains("view full coverage")) continue;
            int end=description.getSpanEnd(span);int newline=description.toString().indexOf('\n',end);
            String source=NewsItem.normalize(description.subSequence(end,newline<0?description.length():newline).toString());
            String entry=title+(source.isEmpty() || source.length()>100?"":"\n"+source);
            if(!result.contains(entry)) result.add(entry);
            if(result.size()==5) break;
        }
        return result;
    }
    private void requestRefresh() {
        if(refreshing) return;refreshStarted=System.currentTimeMillis();refreshing=true;NewsJobs.requestNow(this);
        if(status!=null) status.setText("Refreshing headlines…");if(refreshButton!=null) refreshButton.setText("Refreshing…");
        handler.removeCallbacks(refreshTimeout);handler.postDelayed(refreshTimeout,22_000);
    }
    private void cacheChanged() {
        if(!resumed) return;
        if(news.fetchedAt()>=refreshStarted || news.failed()) { refreshing=false;handler.removeCallbacks(refreshTimeout); }
        if(selected==null) showHome(scroll.getScrollY()); // Preserve the reading position when new data arrives.
        else if(status!=null && "Refreshing headlines…".contentEquals(status.getText())) status.setText(news.failed()?"Offline · showing cached coverage":"Headlines updated");
    }
    private void updateStatus() {
        if(status==null || selected!=null) return;
        if(refreshing) status.setText("Refreshing headlines…");
        else if(news.fetchedAt()==0) status.setText(news.failed()?"Offline · refresh to retry":"Canada · English");
        else status.setText((news.failed()?"Offline · cached ":"Updated ")+ReaderFormat.elapsed(news.fetchedAt(),System.currentTimeMillis()));
    }
    private void openOnPhone(NewsItem item) {
        if(!ReaderFormat.safeLink(item.link)) return;
        TextView feedback=status;feedback.setText("Sending to your phone…");
        TextView button=findViewById(R.id.reader_phone);button.setEnabled(false);
        try {
            ListenableFuture<Void> result=new RemoteActivityHelper(this,remoteExecutor).startRemoteActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(item.link)).addCategory(Intent.CATEGORY_BROWSABLE),null);
            result.addListener(() -> {
                String message;
                try { result.get();message="Sent to your phone"; }
                catch(Exception unavailable) { message="Phone unavailable. Connect your phone and try again."; }
                String text=message;handler.post(() -> { if(!isDestroyed()) { feedback.setText(text);button.setEnabled(true); } });
            },remoteExecutor);
        } catch(Exception unavailable) { feedback.setText("Phone unavailable. Connect your phone and try again.");button.setEnabled(true); }
    }
    private void goBack() { if(selected!=null) showHome(homeScroll);else finish(); }
    @Override public boolean onGenericMotionEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            float delta=-event.getAxisValue(MotionEvent.AXIS_SCROLL)*ViewConfiguration.get(this).getScaledVerticalScrollFactor();
            scroll.smoothScrollBy(0,Math.round(delta));return true;
        }
        return super.onGenericMotionEvent(event);
    }
}
