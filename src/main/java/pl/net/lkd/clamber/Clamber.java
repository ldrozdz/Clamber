package pl.net.lkd.clamber;

import pl.net.lkd.clamber.beans.CrawlConfig;
import pl.net.lkd.clamber.crawlers.Crawler;
import pl.net.lkd.clamber.crawlers.JSoupCrawler;

import java.util.regex.Pattern;

public class Clamber {

    //Przerobić *_TO_RECORD na Writer -> *_PATTERNS
    public static void main(String[] args) {
        CrawlConfig config = new CrawlConfig();
        config.setStartURL("http://www.ema.europa.eu/ema");
        config.setOutFile("/tmp/emea.csv");
        config.setPatternsToRecord(Pattern.compile(".*(pages/medicines).*"));
        config.setTimeout(10000);
        Crawler crawler = new JSoupCrawler(config);
        crawler.crawl();
    }

}
