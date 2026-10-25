package com.nocturne.news;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

/** Saved snapshots are independent of the rolling feed cache. */
public final class ReaderStore {
    private final SharedPreferences prefs;
    public ReaderStore(Context context) { prefs=context.getSharedPreferences("reader",Context.MODE_PRIVATE); }
    public int textSize() { return prefs.getInt("text_size",16)>=20?20:16; }
    public void textSize(int size) { prefs.edit().putInt("text_size",size>=20?20:16).apply(); }
    public List<NewsItem> saved() {
        List<NewsItem> result=new ArrayList<>();
        try {
            JSONArray array=new JSONArray(prefs.getString("saved","[]"));
            for(int i=0;i<Math.min(100,array.length());i++) result.add(NewsStore.decode(array.getJSONObject(i)));
        } catch(JSONException ignored) { result.clear(); }
        return result;
    }
    public boolean isSaved(NewsItem item) { for(NewsItem saved:saved()) if(key(saved).equals(key(item))) return true; return false; }
    public void toggle(NewsItem item) {
        List<NewsItem> entries=saved(); boolean existed=entries.removeIf(e -> key(e).equals(key(item)));
        if(!existed) entries.add(0,item);
        JSONArray array=new JSONArray();
        try { for(int i=0;i<Math.min(100,entries.size());i++) array.put(NewsStore.encode(entries.get(i))); }
        catch(JSONException failure) { throw new IllegalStateException("Unable to save bulletin",failure); }
        prefs.edit().putString("saved",array.toString()).apply();
    }
    private static String key(NewsItem item) { return item.link.isEmpty()?item.title:item.link; }
}
