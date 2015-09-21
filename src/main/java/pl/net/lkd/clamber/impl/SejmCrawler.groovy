package pl.net.lkd.clamber.impl

import groovy.util.logging.Slf4j
import groovyx.gpars.GParsPool
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.remote.DesiredCapabilities

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Created by lukasz on 05/02/15.
 */
@Slf4j
class SejmCrawler {

    enum RomanDigits {
        I(1), V(5), X(10), L(50), C(100), D(500), M(1000);

        private magnitude
        private static symbols = [1: 'I', 4: 'IV', 5: 'V', 9: 'IX', 10: 'X', 40: 'XL', 50: 'L', 90: 'XC', 100: 'C', 400: 'CD', 500: 'D', 900: 'CM', 1000: 'M']

        private RomanDigits(magnitude) { this.magnitude = magnitude }

        String toString() { super.toString() + "=${magnitude}" }

        static BigInteger parse(String numeral) {
            assert numeral != null && !numeral.empty
            def digits = (numeral as List).collect {
                RomanDigits.valueOf(it)
            }
            def L = digits.size()
            (0..<L).inject(0g) { total, i ->
                def sign = (i == L - 1 || digits[i] >= digits[i + 1]) ? 1 : -1
                total + sign * digits[i].magnitude
            }
        }

        static String parse(BigInteger arabic) {
            def result = ""
            symbols.keySet().sort().reverse().each {
                while (arabic >= it) {
                    arabic -= it
                    result += symbols[it]
                }
            }
            return result
        }
    }

    public static Map queryAsMap(String url) { queryAsMap(new URL(url)) }

    public static Map queryAsMap(URL url) {
        return url.query.split('&').inject([:]) { map, kv ->
            def (key, value) = kv.split('=').toList()
            if (value != null) {
                map[key] = URLDecoder.decode(value, 'UTF-8')
                return map
            }
        }
    }

    static final POOL_SIZE = 4
    static final NUM_PAGES = 4
    static final TERM = 7
    static final WAIT_TIME = 15
    static final mainUrl = "http://www.sejm.gov.pl/sejm7.nsf"

    private static Map crawlSitting(def driver, def url) {
        Collection.metaClass.collectWithIndex = { body ->
            def i = 0
            delegate.collect { body(it, i++) }
        }

        //Crawl items
        log.info("\tCrawling ${url}")
        driver.get(url)
        Document doc = Jsoup.parse(driver.pageSource) //Jsoup.connect(url).timeout(WAIT_TIME * 1000).get()
        def uttMap = [:]
        doc.select('table.list > tbody > tr').each {
            def whoEl = it.child(0).child(0)
            def uttUrl = queryAsMap("${mainUrl}/${whoEl.attr('href')}")
            def n = it.child(1).text() ? (it.child(1).text() =~ /(\d+)/)[0][0] as Integer : null
            def title = it.child(2).text()?.trim()
            if (title) {
                uttMap << [(uttUrl.wyp as Integer): [n    : n,
                                                     title: title]]
            }
        }

        def items = (uttMap.values() as Set).findAll { it.title }.collectWithIndex { utt, idx ->
            return [n     : utt.n,
                    seq   : idx + 1,
                    title : utt.title,
                    xml_id: "txt_${utt.n}-item"]
        }

        //Crawl utterances
        def mkid = {
            return Normalizer.normalize(it.tr('ąćęłńóśźżĄĆĘŁŃÓŚŹŻ', 'acelnoszzACLNOSZZ'), Normalizer.Form.NFD)
                  .replaceAll(/\(.*\)/, '')
                  .replaceAll("[^\\p{ASCII}]", "")
                  .replaceAll(/\s/, '')
        }

        def crawlUtterance = { u ->
//            log.info("\t\tCrawling ${u}")
            driver.get(u)
            Document d = Jsoup.parse(driver.pageSource)//Jsoup.connect(u).timeout(WAIT_TIME * 1000).get()
            return d.select('div.stenogram > p').collect { it.text().trim() }.findAll()
        }

        def q = queryAsMap(url)
        url = "${mainUrl}/wypowiedz.xsp?posiedzenie=${q.posiedzenie}&dzien=${q.dzien}&wyp=0"
        driver.get(url)
        doc = Jsoup.parse(driver.pageSource) //Jsoup.connect(url).timeout(WAIT_TIME * 1000).get()

        def uttN = 1
        def divN = 1
        def whoId = null
        def pMap = [:]
        def utts = []

        doc.select('div.stenogram').first().children().each { el ->
            switch (el.tagName()) {
                case 'h3':
                    uttN = 1
                    divN++

                    def sub = el.select('a')
                    if (sub) {
                        def a = "${mainUrl}/${sub[0].attr('href')}"
                        def n = queryAsMap(a).wyp as Integer
                        def itemId = (n in uttMap) ? "txt_${uttMap[n].n}-item" : null
                        whoId = mkid(el.text())

                        try {
                            utts += crawlUtterance(a).collectWithIndex { t, uidx ->
                                [xml_id     : "u-${divN}.${uttN}",
                                 seq        : uidx + 1,
                                 div_xml_id : "div-${divN}",
                                 div_seq    : divN,
                                 item_xml_id: itemId,
                                 who_xml_id : whoId,
                                 text       : t]
                            }
                        } catch (Exception e) {
                            log.warn("\tException processing ${a}: {}", e)
                        }
                    } else {
                        whoId = mkid(el.text().replace(':', ''))
                    }

                    if (!(whoId in pMap)) {
                        pMap << [(whoId): [xml_id: whoId,
                                           role  : 'speaker',
                                           name  : el.text().replace(':', '')]]
                    }
                    break

                case 'p':
                    utts << [xml_id     : "u-${divN}.${uttN}",
                             seq        : uttN,
                             div_xml_id : "div-${divN}",
                             div_seq    : divN,
                             item_xml_id: null,
                             who_xml_id : whoId,
                             text       : el.text().trim()]
                    break
            }
            uttN++
        }


        def h = [participants: pMap.values() as List]
        return [items: items, utterances: utts, header: h]
    }

    static List crawlMeta(def numPages) {
        WebDriver driver = new PhantomJSDriver(new DesiredCapabilities())
        def meta = []
        //Dirty, dirty hack, but if they can't make a proper webpage, they only have thmeselves to blame
        while (meta.unique().size() <= (numPages - 1) * 30) {
            (1..numPages).each { i ->
                driver.get('http://www.sejm.gov.pl/sejm7.nsf/wypowiedzi.xsp')
                log.info("Getting page ${i}")
                if (i != 1) {
                    List<WebElement> pages = driver.findElements(By.xpath('//a[@href="#"]'))
                    pages[i - 2].click()
                    TimeUnit.SECONDS.sleep(WAIT_TIME)
                }
                def entries = driver.findElements(By.xpath('//table[@class="list"]/tbody/tr'))
                entries.each { e ->
                    def title = e.findElement(By.tagName('td')).text
                    def links = e.findElements(By.xpath('./td[2]/a'))
                          .collect { [url: it.getAttribute('href'), text: it.text] }
                    meta << [session: title, days: links]
                }
            }
            log.info("Iteration done, got ${meta.unique().size()} entries")
        }
        assert meta.unique().size() > (numPages - 1) * 30
        driver.quit()
        return meta
    }

    static List<Map> crawl(String... parseOnlyThese = []) {
        def pat = /(.*)(?: nr )(\d+)/

        def plDateParse = { dt ->
            return LocalDate.parse(dt, DateTimeFormatter.ofPattern("d MMMM yyyy (EEEE)", new Locale("pl")))
        }

        def plDateFormat = { dt ->
            return "${dt.toLocalDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("pl")))} r."
        }

        def enDateFormat = { dt ->
            return dt.toLocalDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("en")))
        }

        def makeXmlId = { type, term, session, day ->
            "PSC_${String.format('%02d', term)}-${type.tokenize().take(2).collect { it[0].toLowerCase() }.join()}-${String.format('%03d', session)}-${String.format('%02d', day)}"
        }

        def makeType = { session ->
            def type = (session ==~ pat) ? (session =~ pat)[0][1] : session
            return (type == 'Posiedzenie') ? 'Posiedzenie Plenarne' : type
        }

        def docs = []

        def meta = crawlMeta(NUM_PAGES)
        GParsPool.withPool(POOL_SIZE) {

            meta.collate(POOL_SIZE).eachParallel { batch ->
                WebDriver driver = new PhantomJSDriver(new DesiredCapabilities())
                batch.each { el ->
                    def session = el.session
                    def days = el.days
                    def sittingNum = queryAsMap(days[0].url).posiedzenie
                    try {
                        if (!parseOnlyThese || (sittingNum in parseOnlyThese)) {
                            def crawled = []
                            days.eachWithIndex { d, di ->
                                def h = [:]
                                h.publisher_pl = 'Kancelaria Sejmu Rzeczypospolitej Polskiej'
                                h.publisher_en = 'Chancellery of the Sejm'
                                h.taxonomy_type = '#typ_qmow'
                                h.taxonomy_channel = '#kanal_prasa_inne'
                                h.revision_desc = """<xml:revisionDesc xmlns:xml='http://www.tei-c.org/ns/1.0'>
                                    <xml:change when='${new java.util.Date().format('yyyy-MM-dd')}' who='#ldrozdz'>Automatic creation of text_structure.xml from the corresponding Sejm protocols.</xml:change>
                                    </xml:revisionDesc>"""
                                h.type = makeType(session)
                                h.term = TERM
                                h.session = (sittingNum =~ /(\d+)/)[0][1] as Short//(session ==~ pat) ? (session =~ pat)[0][2] as Short : 0
                                h.day = di + 1
                                h.date = java.sql.Date.valueOf(plDateParse(d.text))
                                h.xml_id = makeXmlId(h.type, h.term, h.session, h.day)
                                h.title_pl = "Sprawozdanie stenograficzne z obrad Sejmu RP z ${plDateFormat(h.date)} (kadencja ${RomanDigits.parse(h.term)}, ${h.type} ${h.session}, dzień ${h.day})."
                                h.title_en = "The Polish Sejm sitting shorthands record, ${enDateFormat(h.date)} (term ${RomanDigits.parse(h.term)}, Session ${h.session}, day ${h.day})."
                                h.title_sub_pl = h.title_pl
                                h.title_sub_en = h.title_en

                                Map s = crawlSitting(driver, "${d.url}")
                                def p = s.header.participants
                                s.header = h
                                s.header.participants = p
                                crawled << s
                            }
                            docs += crawled
                        }
                    }
                    catch (Exception e) {
                        log.error("Error processing ${el.session}: {}", e)
                    }
                }
                driver.quit()
            }
        }

        log.info("Done!")
        return docs
    }

}
