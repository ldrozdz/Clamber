package pl.net.lkd.clamber.impl

import groovy.util.logging.Slf4j
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Slf4j
class PBICrawler {
    static p = null;
    static s = 1;
    static pbiUrl = "http://www.pbi.edu.pl/content.php?p=${-> p}&s=${-> s}";

    public static void crawl(def id) {
        p = id;
        Document doc = Jsoup.connect(pbiUrl).get();
        def meta = doc.select("span.t11-green").collect { it.text() }
        def title = meta[0];
        def author = meta[2].replaceAll(' ', '_').replaceAll("[^a-zA-Z0-9_-]", "");
        def numPages = meta[1].split('/')[-1];
        def text = [];
        (1..(numPages as int)).each { i ->
            log.info("Processing ${author}, ${title} [${i}/${numPages}].");
            s = i;
            doc = Jsoup.connect(pbiUrl).get();
            doc.select('span.txt').each {
                text << it.text().trim();
            }
        }
        new File("/tmp/${sanitize(author)}_${sanitize(title)}.txt").withWriter {
            it.write(text.join('\n'));
        }
    }

    private static String sanitize(String s) {
        return s.replaceAll(' ', '_').replaceAll("[^a-zA-Z0-9_-]", "");
    }

    public static void getMeta(def url) {
        Document doc = Jsoup.connect(url).get();
        doc.select('a[href^=javascript:showPublication]').each {
            def td = it.parent();
            def a = td.select('a[href^=javascript:showPublication]').first();
            def title = a.text();
            def id = (a.attr('href') =~ /(\d+)/)[0][0];
            def author = td.select('b').first().text();
            println "${id},${author},${title}";
        }
    }
}
