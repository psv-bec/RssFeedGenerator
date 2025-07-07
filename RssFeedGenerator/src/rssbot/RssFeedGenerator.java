package rssbot;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class RssFeedGenerator {
    private static final String INPUT_FEED_URL = "https://srinijobpostings.blogspot.com/feeds/posts/default?alt=rss";
    private static final String OUTPUT_RSS_FILE = "feed/blog_feed.xml";

    public static void main(String[] args) {
        try {
            List<Item> recentPosts = fetchRecentBlogPosts();
            if (recentPosts.isEmpty()) {
                System.out.println("No recent posts found in the last 24 hours.");
            } else {
                writeFeed(recentPosts);
                System.out.println("✅ RSS feed created: " + OUTPUT_RSS_FILE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Item> fetchRecentBlogPosts() throws Exception {
        List<Item> items = new ArrayList<>();

        URL url = new URL(INPUT_FEED_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(conn.getInputStream());

        NodeList nodeList = doc.getElementsByTagName("item");
        long now = System.currentTimeMillis();
        long twentyFourHoursAgo = now - (24L * 60 * 60 * 1000);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element item = (Element) nodeList.item(i);
            String title = getTagValue(item, "title");
            String link = getTagValue(item, "link");
            String description = getTagValue(item, "description");
            String pubDate = getTagValue(item, "pubDate");

            ZonedDateTime publishedTime = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            long publishedMillis = publishedTime.toInstant().toEpochMilli();

            if (publishedMillis >= twentyFourHoursAgo) {
                items.add(new Item(title, link, description, pubDate));
            }
        }

        return items;
    }

    private static void writeFeed(List<Item> items) throws Exception {
        Files.createDirectories(Paths.get("feed"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element rss = doc.createElement("rss");
        rss.setAttribute("version", "2.0");
        doc.appendChild(rss);

        Element channel = doc.createElement("channel");
        rss.appendChild(channel);

        addElement(doc, channel, "title", "Recent Blog Posts");
        addElement(doc, channel, "link", "https://yourblog.blogspot.com");
        addElement(doc, channel, "description", "Blogger posts from the last 24 hours");

        for (Item item : items) {
            Element itemElement = doc.createElement("item");
            addElement(doc, itemElement, "title", item.title);
            addElement(doc, itemElement, "link", item.link);
            addElement(doc, itemElement, "description", item.description);
            addElement(doc, itemElement, "pubDate", item.pubDate);
            channel.appendChild(itemElement);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        try (FileOutputStream out = new FileOutputStream(OUTPUT_RSS_FILE)) {
            transformer.transform(new DOMSource(doc), new StreamResult(out));
        }
    }

    private static void addElement(Document doc, Element parent, String name, String text) {
        Element element = doc.createElement(name);
        element.appendChild(doc.createTextNode(text));
        parent.appendChild(element);
    }

    private static String getTagValue(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        return list.getLength() > 0 ? list.item(0).getTextContent() : "";
    }

    static class Item {
        final String title, link, description, pubDate;
        Item(String t, String l, String d, String p) {
            title = t; link = l; description = d; pubDate = p;
        }
    }
}
