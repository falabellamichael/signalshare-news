package com.nocturne.news;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NewsParser {
    public static final int MAX_BYTES = 1_048_576;
    private NewsParser() {}
    public static List<NewsItem> parse(byte[] xml) throws Exception {
        if (xml.length > MAX_BYTES) throw new IOException("Feed exceeds size limit");
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(new ByteArrayInputStream(xml), null);
        List<NewsItem> items = new ArrayList<>();
        Set<String> titles = new HashSet<>();
        String title="", source="", link="", published="", coverage="", field="";
        StringBuilder value = new StringBuilder();
        int itemDepth = -1, events = 0;
        for (int event=parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event=parser.nextToken()) {
            if (++events > 30000 || parser.getDepth() > 32) throw new IOException("Feed exceeds structure limit");
            if (event == XmlPullParser.DOCDECL) throw new IOException("DTD is not allowed");
            if (event == XmlPullParser.START_TAG) {
                if ("item".equals(parser.getName()) && itemDepth < 0) {
                    itemDepth=parser.getDepth(); title=""; source=""; link=""; published=""; coverage="";
                } else if (itemDepth >= 0 && parser.getDepth() == itemDepth+1) {
                    field=parser.getName(); value.setLength(0);
                }
            } else if (itemDepth >= 0 && (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT || event == XmlPullParser.ENTITY_REF)) {
                if (parser.getDepth() == itemDepth+1 && parser.getText() != null) value.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG && itemDepth >= 0) {
                if (parser.getDepth() == itemDepth+1) {
                    switch(field) {
                        case "title": title=value.toString(); break;
                        case "source": source=value.toString(); break;
                        case "link": link=value.toString(); break;
                        case "pubDate": published=value.toString(); break;
                        case "description": coverage=value.toString(); break;
                        default: break;
                    }
                    field=""; value.setLength(0);
                } else if (parser.getDepth() == itemDepth && "item".equals(parser.getName())) {
                    NewsItem item = new NewsItem(title, source, link, published, coverage);
                    if (!item.title.isEmpty() && item.title.length() <= 2000 && titles.add(item.title)) items.add(item);
                    itemDepth=-1;
                    if (items.size() == 20) break;
                }
            }
        }
        if (items.isEmpty()) throw new IOException("Feed contains no headlines");
        return items;
    }
}
