package com.nocturne.news;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NewsStore {
    private final SharedPreferences prefs;
    private final Context context;
    private static final Object LOCK = new Object();
    public NewsStore(Context context) {
        this.context=context.getApplicationContext();
        prefs=this.context.getSharedPreferences("news", Context.MODE_PRIVATE);
    }
    public long fetchedAt() { return prefs.getLong("fetched_at",0); }
    public long attemptedAt() { return prefs.getLong("attempted_at",0); }
    public boolean failed() { return prefs.getBoolean("failed",false); }
    public void attempted() { prefs.edit().putLong("attempted_at",System.currentTimeMillis()).apply(); }
    public void failedFetch() { prefs.edit().putBoolean("failed",true).apply(); notifyFace(); }
    public List<NewsItem> items() {
        List<NewsItem> result=new ArrayList<>();
        try {
            JSONArray array=new JSONArray(prefs.getString("items","[]"));
            for(int i=0;i<array.length();i++) {
                JSONObject e=array.getJSONObject(i);
                result.add(decode(e));
            }
        } catch(JSONException ignored) { result.clear(); }
        return result;
    }
    public void save(List<NewsItem> items) throws JSONException {
        if(items.isEmpty()) throw new IllegalArgumentException("Cannot replace cache with no headlines");
        JSONArray array=new JSONArray();
        for(NewsItem item:items) array.put(encode(item));
        prefs.edit().putString("items",array.toString()).putLong("fetched_at",System.currentTimeMillis()).putBoolean("failed",false).apply();
        notifyFace();
    }
    public void active(int id, boolean active) {
        synchronized(LOCK) {
            Set<String> ids=new HashSet<>(prefs.getStringSet("active",Set.of()));
            if(active) ids.add(Integer.toString(id)); else ids.remove(Integer.toString(id));
            prefs.edit().putStringSet("active",ids).apply();
        }
    }
    public boolean hasActive() { return !prefs.getStringSet("active",Set.of()).isEmpty(); }
    public void observe(SharedPreferences.OnSharedPreferenceChangeListener listener) { prefs.registerOnSharedPreferenceChangeListener(listener); }
    public void stopObserving(SharedPreferences.OnSharedPreferenceChangeListener listener) { prefs.unregisterOnSharedPreferenceChangeListener(listener); }
    public static JSONObject encode(NewsItem item) throws JSONException {
        return new JSONObject().put("title",item.title).put("source",item.source).put("link",item.link).put("published",item.published).put("coverage",item.coverage);
    }
    public static NewsItem decode(JSONObject item) throws JSONException {
        return new NewsItem(item.getString("title"),item.optString("source"),item.optString("link"),item.optString("published"),item.optString("coverage"));
    }
    private void notifyFace() {
        ComplicationDataSourceUpdateRequester.create(context,new ComponentName(context,NewsComplicationService.class)).requestUpdateAll();
    }
}
