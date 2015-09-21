package pl.net.lkd.clamber.crawlers;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pl.net.lkd.clamber.beans.CrawlConfig;
import pl.net.lkd.clamber.beans.Link;

//If only the Jaunt API were open source, it could be an interesting option instead of JSoup

@Slf4j
public class JSoupCrawler extends Crawler {

    public JSoupCrawler(CrawlConfig _config) {
        super(_config);
    }

    @Override
    Link visit(Link link) {
        try {
            Connection.Response resp = Jsoup.connect(link.getHref())
                  .timeout(config.getTimeout())
                  .ignoreContentType(true)
                  .execute();
            link.setMimetype(resp.contentType());
            if (config.getMimetypesToVisit().matcher(link.getMimetype()).matches() &&
                  config.getPatternsToVisit().matcher(link.getHref()).matches()) {
                Document doc = resp.parse();
                String title = doc.title();
                for (Element l : doc.select("a[href]")) {
                    String href = l.attr("abs:href");
                    String linkText = l.text();
                    if (i < config.getLimit() && href.startsWith(domain) && !visited.contains(href)) {
                        queue.add(new Link(title, linkText, href, null));
                    }
                    i++;
                    if (i % 100 == 0) {
                        log.info("Crawled {} pages.", i);
                    }
                    if (config.isDebug()) {
                        System.out.println("[" + i + "] " + href + " " + linkText);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error at URL {}", link.getHref(), e);
        }
        return link;
    }

}