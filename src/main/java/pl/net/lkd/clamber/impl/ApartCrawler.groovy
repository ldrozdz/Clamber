package pl.net.lkd.clamber.impl

import groovy.json.JsonBuilder
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.util.logging.Slf4j
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Slf4j
class ApartCrawler {

    private static final Template BASE_URL = new SimpleTemplateEngine()
          .createTemplate('http://www.realestate-slovenia.info/nepremicnine.html?d=$d&p=$p&n=$n&r=$r')
    private static final COUNTRIES = [197: 'Slovenia',
                                      55 : 'Croatia',
                                      999: 'other']
    private static final OFFER_TYPES = [1: 'for_sale',
                                        2: 'buying',
                                        3: 'for_rent',
                                        4: 'leasehood']
    private static final PROPERTY_TYPES = [1: 'apartment',
                                           2: 'house',
                                           4: 'cottage_house',
                                           3: 'land',
                                           5: 'business_premise',
                                           6: 'garage',
                                           7: 'holiday_home']
    private static final REGIONS = [14: 'LJ-city',
                                    1 : 'LJ-surroundings',
                                    3 : 'Gorenjska',
                                    2 : 'S. Primorska',
                                    4 : 'N. Primorska',
                                    8 : 'Notranjska',
                                    5 : 'Savinjska',
                                    9 : 'Podravska',
                                    10: 'Koroška',
                                    6 : 'Dolenjska',
                                    12: 'Posavska',
                                    11: 'Zasavska',
                                    15: 'Pomurska']

    private static class Offer {
        String id
        String location
        String offerType
        String propertyType
        String region
        String country
        String size
        String price
        String attributes
        String description
        String seller
        String url
    }

    /**
     *
     * @param country country
     * @param offerType offer type
     * @param propertyType property type
     * @param region region
     */
    private static List<Offer> crawl(Integer country, Integer offerType, Integer propertyType, Integer region) {
        def url = BASE_URL.make(d: country, p: offerType, n: propertyType, r: region).toString()
        log.info("Getting ${url}")
        def offers = []
        try {
            Document doc = Jsoup.connect(url).get()
            offers = doc.select('div.oglas_container').collect { div ->
                def id = div.select('h2 > a').attr('title')
                def location = div.select('h2').text()
                def link = div.select('h2 > a').attr('abs:href')
                def size = div.select('span.velikost').text()
                def price = div.select('span.cena').text()
                def desc = div.select('div.kratek').text()
                def attrs = div.select('span.atribut')*.text().join(', ')
                def seller = div.select('div.prodajalec_o').text()
                return new Offer(id: id,
                                 location: location,
                                 offerType: OFFER_TYPES[offerType],
                                 propertyType: PROPERTY_TYPES[propertyType],
                                 region: REGIONS[region],
                                 country: COUNTRIES[country],
                                 size: size,
                                 price: price,
                                 description: desc,
                                 attributes: attrs,
                                 seller: seller,
                                 url: link)
            }
        } catch (Exception e) {
            log.warn("Error getting ${url}: {}", e)
        }
        return offers
    }

    private static void serialize(List<Offer> offers, String path) {
        try {
            new File(path).write(new JsonBuilder(offers).toPrettyString())
        } catch (Exception e) {
            log.warn("Error writing JSON: {}", e)
        }
    }

    public static void main(String[] args) {
        def d = 197
        def path = '/tmp/aparts.json'
        def data = []
        [OFFER_TYPES.keySet(), PROPERTY_TYPES.keySet(), REGIONS.keySet()].combinations().each { p, n, r ->
            data += crawl(d, p, n, r)
        }
        serialize(data, path)
    }
}
