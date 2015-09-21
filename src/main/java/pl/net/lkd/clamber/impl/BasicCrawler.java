package pl.net.lkd.clamber.impl;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
public class BasicCrawler extends WebCrawler {

    private static final boolean DEBUG = false;

    private static Pattern FILTERS = Pattern.compile(".*(\\.(css|bmp|gif|jpe?g" + "|png|tiff?|mid|mp2|mp3|mp4"
          + "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
    private static File storageFolder;
    private static String[] crawlDomains;

    private static Connection db = null;
    private static final String insertRecord = "INSERT INTO paralela_dev.emea_dirty(parent, name, href) VALUES(?, ?, ?)";
    private static AtomicInteger i = new AtomicInteger(0);

    public static void configure(String dbProps, String[] domain, String storageFolderName, String filters) {
        db = null;

        BasicCrawler.crawlDomains = domain;

        storageFolder = new File(storageFolderName);
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }

        if (StringUtils.isNoneBlank(filters)) {
            FILTERS = Pattern.compile(filters);
        }
    }

    /**
     * You should implement this function to specify whether the given url
     * should be crawled or not (based on your crawling logic).
     */
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL().toLowerCase();
        if (FILTERS.matcher(href).matches()) {
            return false;
        }

        for (String domain : crawlDomains) {
            if (href.startsWith(domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This function is called when a page is fetched and ready to be processed
     * by your program.
     */
    @Override
    public void visit(Page page) {
        int docid = page.getWebURL().getDocid();
        String url = page.getWebURL().getURL();
        String domain = page.getWebURL().getDomain();
        String path = page.getWebURL().getPath();
        String subDomain = page.getWebURL().getSubDomain();
        String parentUrl = page.getWebURL().getParentUrl();
        String anchor = page.getWebURL().getAnchor();
        String title = null;

        if (DEBUG) {
            System.out.println("Docid: " + docid);
            System.out.println("URL: " + url);
            System.out.println("Domain: '" + domain + "'");
            System.out.println("Sub-domain: '" + subDomain + "'");
            System.out.println("Path: '" + path + "'");
            System.out.println("Parent page: " + parentUrl);
            System.out.println("Anchor text: " + anchor);
        }

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            title = htmlParseData.getTitle();
            String text = htmlParseData.getText();
            String html = htmlParseData.getHtml();
            List<WebURL> links = htmlParseData.getOutgoingUrls();

            for (WebURL l : links) {
                Map<String, String> record = new HashMap<>();
                record.put("parent", l.getParentUrl());
                record.put("name", l.getAnchor());
                record.put("href", l.getURL());
                writeRecord(record);
            }

            if (DEBUG) {
                System.out.println("Text length: " + text.length());
                System.out.println("Html length: " + html.length());
                System.out.println("Number of outgoing links: " + links.size());
            }
        }

        Map<String, String> record = new HashMap<>();
        record.put("parent", parentUrl);
        record.put("name", title);
        record.put("href", url);

        writeRecord(record);


        Header[] responseHeaders = page.getFetchResponseHeaders();
        if (DEBUG) {
            if (responseHeaders != null) {
                System.out.println("Response headers:");
                for (Header header : responseHeaders) {
                    System.out.println("\t" + header.getName() + ": " + header.getValue());
                }
            }
            System.out.println("=============");
        }

        int cnt = i.getAndIncrement();
        if (cnt % 100 == 0) {
            log.info("Processed {} pages.", cnt);
        }
    }

    private void writeRecord(Map<String, String> record) {
        if (db != null) {
            try {
                PreparedStatement ps = db.prepareStatement(insertRecord);
                ps.setString(1, record.get("parent"));
                ps.setString(2, record.get("name"));
                ps.setString(3, record.get("href"));
                ps.executeUpdate();
            } catch (SQLException e) {
                log.error("DB operation failed: {}", e);
            }
        }
    }
}
