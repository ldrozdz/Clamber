package pl.net.lkd.clamber.crawlers;

import lombok.extern.slf4j.Slf4j;
import pl.net.lkd.clamber.beans.CrawlConfig;
import pl.net.lkd.clamber.beans.Link;
import pl.net.lkd.clamber.writers.Writer;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Slf4j
public abstract class Crawler {
  static CrawlConfig config;
  static String domain = null;
  static int i = 0;
  static Set<String> visited = new HashSet<>();
  static Queue<Link> queue = new LinkedList<>();

  public Crawler(CrawlConfig _config) {
    config = _config;
  }

  void init() {}
  void close() {}
  abstract Link visit(Link link);

  public void crawl() {
    if (config.getStartURL() == null || config.getOutFile() == null) {
      log.info("You need to provide at least the 'startURL' and 'outFile' properties!");
    } else {
      try {
        init();
        URI uri = new URI(config.getStartURL());
        domain = String.format("%s://%s", uri.getScheme(), uri.getHost());
        queue.add(new Link("", "Home", config.getStartURL(), "text/html"));

        Writer writer = config.getWriter();
        writer.init(config);
        try {
          while (!queue.isEmpty() && i < config.getLimit()) {
            Link linkToVisit = queue.poll();
            Link visitedLink = visit(linkToVisit);
            visited.add(visitedLink.getHref());
            writer.writeRecord(visitedLink);
          }
        } catch (Exception e) {
          log.error("{}", e);
        }
        log.info("Crawled {} pages", i);
        writer.close();
        close();
      } catch (Exception e) {
        log.error("{}", e);
      }
    }
  }

}