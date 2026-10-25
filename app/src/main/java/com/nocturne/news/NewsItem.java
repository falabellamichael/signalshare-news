package com.nocturne.news;

public final class NewsItem {
    public final String title, source, link, published, coverage;
    public NewsItem(String title, String source, String link, String published) {
        this(title,source,link,published,"");
    }
    public NewsItem(String title, String source, String link, String published, String coverage) {
        this.title = normalize(title); this.source = normalize(source);
        this.link = normalize(link); this.published = normalize(published);
        this.coverage = coverage==null ? "" : coverage.substring(0,Math.min(coverage.length(),24000));
    }
    public static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\p{Cc}&&[^\\n\\t]]", "").replaceAll("\\s+", " ").trim();
    }
    public String displayTitle() {
        String suffix = " - " + source;
        return !source.isEmpty() && title.endsWith(suffix) ? title.substring(0, title.length()-suffix.length()) : title;
    }
    public static int indexAt(long timeMillis, int count) {
        return count <= 0 ? 0 : (int) Math.floorMod(Math.floorDiv(timeMillis, 300_000), count);
    }
}
