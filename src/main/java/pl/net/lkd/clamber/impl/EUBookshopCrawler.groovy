package pl.net.lkd.clamber.impl

import groovy.util.logging.Slf4j
import groovyx.net.http.HTTPBuilder
import org.jsoup.Jsoup

@Slf4j
class EUBookshopCrawler {

    public static void crawl(def dbProps) {
        def PART = 1;

        def ids = getIds();
        log.info("${ids.flatten().size()} texts found.");

        def docs = [:];

        def http = new HTTPBuilder('http://bookshop.europa.eu');

        new File(String.format('/tmp/eubks%02d.txt', PART)).withWriter { f ->
            ids.each { id ->
                log.info("Getting data for doc ${id}.");
                def q = ['webform-id'           : 'WFSimpleSearch',
                         DefaultButton          : 'findSimple',
                         WFSimpleSearch_NameOrID: id,
                         SearchConditions       : '',
                         SearchType             : '1',
                         SortingAttribute       : 'LatestYear-desc',
                         'findSimple.x'         : '0',
                         'findSimple.y'         : '0',
                         findSimple             : 'GO'];

                def listing = http.get(path: '/en/search/', query: q);
                try {
                    def pub = listing.'**'.findAll { it.name() == 'UL' && it.@id == 'publications' }?.get(0);
                    if (pub) {
                        def url = pub.LI[0].H4[0].A[0].@href.text();
                        ['/en/', '/pl/'].each { lang ->
                            url = (url =~ /\/en\//).replaceFirst(lang);
                            log.info("Getting ${url}.");
                            def page = Jsoup.parse(url.toURL(), 60000);

                            def p = page.select('div#publication').first();
                            def desc = p.select('div#description>h1').text();
                            def authors = p.select('dl#authors > dd > a').collect { it.text() }.join('|');
                            def info = page.select('dd[id$=PDF] > table.editions-info') ?: page.select('dd[id$=PAPER] > table.editions-info');
                            def dt = (info.select('td.edition-information>p').text() =~ /(\d+)/)[0][0];
                            def num = info.select('td.catalogue-number').text();
                            def dl = info.select('dd.download a.download-file').attr('href');

                            def doc = [title  : desc,
                                       authors: authors,
                                       date   : dt,
                                       id     : num,
                                       url    : dl];
                            docs << doc;

                            f.writeLine("${id}\t${doc.id}\t${doc.title}\t${doc.date}\t${doc.authors}\t${doc.url}")

                        }
                    } else {
                        log.warn("URL not found for ${id}!");
                    }
                } catch (Exception e) {
                    log.warn("Exception processing text ${id}:", e);
                }
            }
        }
    }


}
