package dk.sonderborg.borger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedParserTest {
    private val src = FeedSource(id = "t", name = "Test", url = "https://example.dk/feed")

    @Test
    fun parsesRss2() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/">
              <channel>
                <title>Test</title>
                <item>
                  <title>Ny cykelsti i Nordborg</title>
                  <link>https://example.dk/a</link>
                  <description><![CDATA[<p>Byrådet har <b>vedtaget</b> en sti &amp; mere.</p>]]></description>
                  <pubDate>Mon, 02 Nov 2026 10:15:00 +0100</pubDate>
                  <guid>a-1</guid>
                </item>
                <item>
                  <title>Ældre nyhed</title>
                  <link>https://example.dk/b</link>
                  <pubDate>Sun, 01 Nov 2026 08:00:00 GMT</pubDate>
                </item>
              </channel>
            </rss>
        """.trimIndent()
        val items = FeedParser.parse(xml.toByteArray(), src)
        assertEquals(2, items.size)
        val a = items[0]
        assertEquals("Ny cykelsti i Nordborg", a.title)
        assertEquals("https://example.dk/a", a.link)
        assertEquals("Byrådet har vedtaget en sti & mere.", a.summary)
        assertEquals("t|a-1", a.id)
        assertTrue(a.publishedMillis > items[1].publishedMillis)
    }

    @Test
    fun parsesAtom() {
        val xml = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom</title>
              <entry>
                <title type="html">Vejarbejde p&#229; Als</title>
                <link rel="alternate" href="https://example.dk/atom-1"/>
                <id>urn:1</id>
                <updated>2026-11-03T12:00:00+01:00</updated>
                <summary>Kort tekst</summary>
              </entry>
            </feed>
        """.trimIndent()
        val items = FeedParser.parse(xml.toByteArray(), src)
        assertEquals(1, items.size)
        assertEquals("Vejarbejde på Als", items[0].title)
        assertEquals("https://example.dk/atom-1", items[0].link)
        assertTrue(items[0].publishedMillis > 0)
    }

    @Test
    fun discoversFeedInHtml() {
        val html = """<html><head><link rel="alternate" type="application/rss+xml" title="Nyheder" href="/nyheder/rss"></head></html>"""
        assertTrue(FeedParser.looksLikeHtml(html.toByteArray()))
        assertEquals("https://kommune.dk/nyheder/rss", FeedParser.discoverFeedUrl(html, "https://kommune.dk/"))
        assertNull(FeedParser.discoverFeedUrl("<html><head></head></html>", "https://kommune.dk/"))
    }

    @Test
    fun localFilter() {
        fun item(t: String) = NewsItem("1", "t", "T", t, "", "https://x", 0)
        assertTrue(FeedParser.isLocal(item("Brand i Sønderborg nat til søndag")))
        assertTrue(FeedParser.isLocal(item("Trafikuheld på ALS")))
        assertTrue(FeedParser.isLocal(item("Ny skole i Gråsten")))
        assertFalse(FeedParser.isLocal(item("Brand i Aabenraa")))
        assertFalse(FeedParser.isLocal(item("Falsk alarm i Haderslev")))
    }

    @Test
    fun mergeSortsAndDedupes() {
        val a = NewsItem("1", "a", "A", "x", "", "https://x.dk/1/", 100)
        val b = NewsItem("2", "b", "B", "x", "", "http://x.dk/1", 50)
        val c = NewsItem("3", "b", "B", "y", "", "https://x.dk/2", 200)
        val merged = FeedParser.merge(listOf(a, b, c))
        assertEquals(listOf("3", "1"), merged.map { it.id })
    }
}
