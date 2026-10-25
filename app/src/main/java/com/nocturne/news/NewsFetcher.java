package com.nocturne.news;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public final class NewsFetcher {
    public static final String FEED="https://news.google.com/rss?hl=en-CA&gl=CA&ceid=CA:en";
    private NewsFetcher() {}
    public static synchronized boolean refresh(Context context) {
        NewsStore store=new NewsStore(context);
        store.attempted();
        HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)new URL(FEED).openConnection();
            connection.setConnectTimeout(7000); connection.setReadTimeout(7000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent","SignalShareNews/1.2");
            connection.setRequestProperty("Accept","application/rss+xml, application/xml, text/xml");
            if(connection.getResponseCode()!=200) throw new IOException("Feed request failed");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream input=connection.getInputStream()) {
                byte[] chunk=new byte[8192]; int count;
                while((count=input.read(chunk))!=-1) {
                    if(Thread.currentThread().isInterrupted()) throw new IOException("Fetch cancelled");
                    if(bytes.size()+count>NewsParser.MAX_BYTES) throw new IOException("Feed exceeds size limit");
                    bytes.write(chunk,0,count);
                }
            }
            store.save(NewsParser.parse(bytes.toByteArray()));
            return true;
        } catch(Exception failure) {
            // Keep the last good headlines; never replace them with a canned live story.
            store.failedFetch(); return false;
        } finally { if(connection!=null) connection.disconnect(); }
    }
}
