package pl.net.lkd.clamber.impl

import edu.uci.ics.crawler4j.crawler.CrawlConfig
import edu.uci.ics.crawler4j.crawler.CrawlController
import edu.uci.ics.crawler4j.fetcher.PageFetcher
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer
import groovy.util.logging.Slf4j
import groovyx.net.http.HTTPBuilder
import lombok.AllArgsConstructor
import lombok.Data
import org.cyberneko.html.parsers.SAXParser

@Slf4j
class EmeaCrawler {

    @Data
    @AllArgsConstructor
    class Link {
        String parent;
        String name;
        String href;
    }

    static def parseWayback() {
        def docs = [];
        def http = new HTTPBuilder('http://archive.org/wayback/available');
        ['human', 'vet'].each { section ->

            ('a'..'z').each { letter ->

                def url = "http://www.ema.europa.eu/htms/${section}/epar/${letter}.htm";
                log.info("Trying http://archive.org/wayback/available/?url=${url}");
                try {
                    def resp = http.get(query: [url: url])
                    if (resp?.archived_snapshots?.closest?.available && resp?.archived_snapshots?.closest?.status == '200') {
                        try {
                            log.info("Getting ${resp.archived_snapshots.closest.url}")
                            def listing = new XmlParser(new SAXParser()).parse(resp.archived_snapshots.closest.url);
                            listing.depthFirst().TR
                                  .grep { it.grep { it.@class == 'tablerows' } }
                                  .grep { it.TD[0].children()[0] in Node }*.TD
                                  .each { tr ->

                                def suburl = tr[0]?.A?.@href[0]?.split('/')[3..-1]?.join('/');
                                def doc = [:];
                                doc.url = suburl;
                                doc.name = tr[0].A.text();
                                doc.date = tr[3].text();

                                if (suburl) {
                                    log.info("\tTrying http://archive.org/wayback/available/?url=${suburl}");
                                    try {
                                        def subResp = http.get(query: [url: suburl]);
                                        if (subResp?.archived_snapshots?.closest?.available && subResp?.archived_snapshots?.closest?.status == '200') {
                                            try {
                                                log.info("\tGetting ${subResp.archived_snapshots.closest.url}")

                                                def details = new XmlParser(new SAXParser()).parse(subResp.archived_snapshots.closest.url);


                                                def headingIndices = [];
                                                details.depthFirst().eachWithIndex { it, idx ->
                                                    if (it in Node && it.@class =~ /eparheading/) {
                                                        headingIndices << idx
                                                    }
                                                };
                                                def headings = headingIndices.collect { details.depthFirst()[it] };
                                                doc.code = headings[1].DIV.text()
                                                doc.subdocs = [];

                                                details.depthFirst().TR
                                                      .grep { it.grep { it.@class == 'epardiamondbkgr' } }
                                                      .grep { it.TD[15].children()[0] in Node }
                                                      .each {
                                                    def subdocUrl = it?.children()[15]?.depthFirst().A.@href[0];
                                                    if (subdocUrl) {
                                                        def subdoc = [:];
                                                        subdoc.url = subdocUrl;
                                                        subdoc.name = it.children()[-1].depthFirst().grep {
                                                            it in Node && it.children()[0] in String && it.name() != 'SCRIPT'
                                                        }*.text().join();
                                                        doc.subdocs << subdoc;
                                                    }
                                                }


                                                [headings.grep { it.@class == 'eparheading3' }, doc.subdocs.grep {
                                                    !it.name || !it.name.replaceAll(/[\p{javaWhitespace}\p{javaSpaceChar}]/, '')
                                                }].transpose().each { h, d ->
                                                    d.name = h.text()?.split()?.join(' ')?.trim()
                                                }
                                                docs << doc;
                                            } catch (Exception e) {
                                                log.warn("{}", e);
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("{}", e);
                                    }
                                }
                                Thread.sleep(1000);
                            }
                        } catch (Exception e) {
                            log.warn("{}", e);
                        }
                    }
                } catch (Exception e) {
                    log.warn("{}", e);
                }
            }
        }
        docs.each {
            println "${it.name.toLowerCase()}\t${it.date}\t${it.code}\t${it.url}"
            it.subdocs.each {
                println "\t${it.name}\t${it.url}"
            }
        };
    }

    def withCrawler4j() {
        String filters = ".*(\\.(css|js|bmp|gif|jpe?g|png|tiff?|mid|mp2|mp3|mp4|wav|pdf" +
              "|avi|mov|mpeg|ram|m4v|rm|smil|wmv|swf|wma|zip|rar|gz))\$";
        String crawlStorageFolder = "/tmp/crawl";
        String dbProps = 'paralela_dev';


        int numberOfCrawlers = 10;
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        config.setPolitenessDelay(100);
        config.setMaxDepthOfCrawling(-1);
        config.setMaxPagesToFetch(-1);
        config.setResumableCrawling(false);
        config.setFollowRedirects(true);
//        config.setMaxOutgoingLinksToFollow(-1);
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        BasicCrawler.configure(dbProps, ["http://www.ema.europa.eu"] as String[], crawlStorageFolder, filters);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        controller.addSeed("http://www.ema.europa.eu/");
        controller.addSeed("http://www.ema.europa.eu/ema/");
        controller.start(BasicCrawler.class, numberOfCrawlers);
    }


}
