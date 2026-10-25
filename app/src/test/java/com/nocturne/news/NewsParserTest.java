package com.nocturne.news;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.Assert.*;

public final class NewsParserTest {
    private List<NewsItem> parse(String body) throws Exception {
        return NewsParser.parse(("<rss><channel>"+body+"</channel></rss>").getBytes(StandardCharsets.UTF_8));
    }
    @Test public void parsesRealRssFieldsAndEntities() throws Exception {
        NewsItem item=parse("<title>Channel title</title><item><title>A &amp; B — 你好 - Publisher</title><source>Publisher</source><link>https://news.google.com/x?a=1&amp;b=2</link><pubDate>Sun, 25 Oct 2026 10:00:00 GMT</pubDate></item>").get(0);
        assertEquals("A & B — 你好",item.displayTitle());assertEquals("Publisher",item.source);
        assertEquals("https://news.google.com/x?a=1&b=2",item.link);
    }
    @Test public void handlesCdataAndDoesNotParseDescriptionAsHeadline() throws Exception {
        NewsItem item=parse("<item><title><![CDATA[Actual headline]]></title><description><![CDATA[<script>bad()</script>]]></description></item>").get(0);
        assertEquals("Actual headline",item.title);
        assertEquals("<script>bad()</script>",item.coverage);
    }
    @Test public void retainsRelatedCoverageAndBoundsEachDescription() throws Exception {
        NewsItem item=parse("<item><title>Lead</title><description><![CDATA[<a href='https://example.com/story'>Related</a>]]></description></item>").get(0);
        assertTrue(item.coverage.contains("Related"));assertEquals("Lead",item.title);
        assertEquals(24000,new NewsItem("T","S","","","a".repeat(25000)).coverage.length());
    }
    @Test public void skipsEmptyAndDuplicateItems() throws Exception {
        assertEquals(1,parse("<item><title> </title></item><item><title>One</title></item><item><title>One</title></item>").size());
    }
    @Test public void boundsItemCount() throws Exception {
        StringBuilder body=new StringBuilder();for(int i=0;i<30;i++)body.append("<item><title>Headline "+i+"</title></item>");
        assertEquals(20,parse(body.toString()).size());
    }
    @Test(expected=Exception.class) public void rejectsEmptyFeed() throws Exception { parse("<title>Channel only</title>"); }
    @Test(expected=Exception.class) public void rejectsMalformedXml() throws Exception { parse("<item><title>Broken</item>"); }
    @Test(expected=Exception.class) public void rejectsExternalEntityDtd() throws Exception {
        NewsParser.parse("<!DOCTYPE rss [<!ENTITY secret SYSTEM 'file:///private'>]><rss><channel><item><title>&secret;</title></item></channel></rss>".getBytes(StandardCharsets.UTF_8));
    }
    @Test(expected=Exception.class) public void rejectsOversizeInput() throws Exception { NewsParser.parse(new byte[NewsParser.MAX_BYTES+1]); }
    @Test public void onlyRemovesExactPublisherSuffix() {
        assertEquals("A - B",new NewsItem("A - B","C","","").displayTitle());
    }
    @Test public void rotatesWithoutForegroundActivity() {
        assertEquals(0,NewsItem.indexAt(0,3));assertEquals(1,NewsItem.indexAt(300_000,3));
        assertEquals(2,NewsItem.indexAt(600_000,3));assertEquals(0,NewsItem.indexAt(900_000,3));
        assertEquals(0,NewsItem.indexAt(300_000,0));
    }
}
