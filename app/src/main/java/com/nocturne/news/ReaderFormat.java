package com.nocturne.news;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class ReaderFormat {
    private ReaderFormat() {}
    public static boolean safeLink(String link) {
        try {
            URI uri=new URI(link);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost()!=null && !uri.getHost().isEmpty() && uri.getUserInfo()==null;
        } catch(Exception invalid) { return false; }
    }
    public static String age(String published,long now) {
        try { return elapsed(ZonedDateTime.parse(published,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli(),now); }
        catch(Exception invalid) { return "Latest bulletin"; }
    }
    public static String elapsed(long timestamp,long now) {
        long minutes=Math.max(0,(now-timestamp)/60_000);
        if(minutes<1) return "Just now";
        if(minutes<60) return minutes+"m ago";
        if(minutes<1440) return minutes/60+"h ago";
        return minutes/1440+"d ago";
    }
}
