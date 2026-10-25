package com.nocturne.news;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ReaderFormatTest {
    @Test public void onlyOpensHttpsLinksWithoutCredentials() {
        assertTrue(ReaderFormat.safeLink("https://news.google.com/rss/articles/example?a=1"));
        assertFalse(ReaderFormat.safeLink("javascript:alert(1)"));assertFalse(ReaderFormat.safeLink("file:///private"));
        assertFalse(ReaderFormat.safeLink("https://user:password@example.com/story"));assertFalse(ReaderFormat.safeLink("https:///missing-host"));
    }
    @Test public void publicationAgeHandlesFutureAndInvalidDates() {
        assertEquals("Just now",ReaderFormat.age("Sun, 25 Oct 2026 10:00:00 GMT",1792922400000L));
        assertEquals("Latest bulletin",ReaderFormat.age("malformed",0));
        assertEquals("Just now",ReaderFormat.elapsed(500_000,100_000));
        assertEquals("1h ago",ReaderFormat.elapsed(0,3_600_000));
        assertEquals("2d ago",ReaderFormat.elapsed(0,172_800_000));
    }
}
