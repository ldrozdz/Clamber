package pl.net.lkd.clamber.impl

import groovy.util.logging.Slf4j
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode

@Slf4j
public class TekstowoCrawler {

    public static void main(String[] args) {
        def outDir = "/tmp/pl00";
        def seeds60 = ["tr" : ["http://www.tekstowo.pl/piosenki_artysty,trubadurzy.html",
                               "http://www.tekstowo.pl/piosenki_artysty,trubadurzy,alfabetycznie,strona,2.html"],
                       "cc1": ["http://www.tekstowo.pl/piosenki_artysty,czerwono_czarni.html"],
                       "cc2": ["http://www.tekstowo.pl/piosenki_artysty,czarno_czarni.html",
                               "http://www.tekstowo.pl/piosenki_artysty,czarno_czarni,alfabetycznie,strona,2.html"],
                       "cg" : ["http://www.tekstowo.pl/piosenki_artysty,czerwone_gitary.html",
                               "http://www.tekstowo.pl/piosenki_artysty,czerwone_gitary,alfabetycznie,strona,2.html",
                               "http://www.tekstowo.pl/piosenki_artysty,czerwone_gitary,alfabetycznie,strona,3.html",
                               "http://www.tekstowo.pl/piosenki_artysty,czerwone_gitary,alfabetycznie,strona,4.html",
                               "http://www.tekstowo.pl/piosenki_artysty,czerwone_gitary,alfabetycznie,strona,5.html",
                               "http://www.tekstowo.pl/piosenki_artysty,czerwone_gitary,alfabetycznie,strona,6.html",
                               "http://www.tekstowo.pl/piosenki_artysty,czerwone_gitary,alfabetycznie,strona,7.html",
                               "http://www.tekstowo.pl/piosenki_artysty,czerwone_gitary,alfabetycznie,strona,8.html"],
                       "sk" : ["http://www.tekstowo.pl/piosenki_artysty,skaldowie.html",
                               "http://www.tekstowo.pl/piosenki_artysty,skaldowie,alfabetycznie,strona,2.html",
                               "http://www.tekstowo.pl/piosenki_artysty,skaldowie,alfabetycznie,strona,3.html"],
                       "ntc": ["http://www.tekstowo.pl/piosenki_artysty,no_to_co.html",
                               "http://www.tekstowo.pl/piosenki_artysty,no_to_co,alfabetycznie,strona,2.html"],
                       "br" : ["http://www.tekstowo.pl/piosenki_artysty,breakout.html",
                               "http://www.tekstowo.pl/piosenki_artysty,breakout,alfabetycznie,strona,2.html"],
                       "sbb": ["http://www.tekstowo.pl/piosenki_artysty,sbb.html"],
                       "ar" : ["http://www.tekstowo.pl/piosenki_artysty,ada_rusowicz.html"],
                       "ks" : ["http://www.tekstowo.pl/piosenki_artysty,karin_stanek.html"]];

        def seeds00 = ["wl": ["http://www.tekstowo.pl/piosenki_artysty,wilki.html",
                              "http://www.tekstowo.pl/piosenki_artysty,wilki,alfabetycznie,strona,2.html",
                              "http://www.tekstowo.pl/piosenki_artysty,wilki,alfabetycznie,strona,3.html"],
                       "mb": ["http://www.tekstowo.pl/piosenki_artysty,monika_brodka.html",
                              "http://www.tekstowo.pl/piosenki_artysty,monika_brodka,alfabetycznie,strona,2.html"],
                       "kk": ["http://www.tekstowo.pl/piosenki_artysty,kasia_kowalska.html",
                              "http://www.tekstowo.pl/piosenki_artysty,kasia_kowalska,alfabetycznie,strona,2.html",
                              "http://www.tekstowo.pl/piosenki_artysty,kasia_kowalska,alfabetycznie,strona,3.html"],
                       "ba": ["http://www.tekstowo.pl/piosenki_artysty,bajm.html",
                              "http://www.tekstowo.pl/piosenki_artysty,bajm,alfabetycznie,strona,2.html",
                              "http://www.tekstowo.pl/piosenki_artysty,bajm,alfabetycznie,strona,3.html",
                              "http://www.tekstowo.pl/piosenki_artysty,bajm,alfabetycznie,strona,4.html",
                              "http://www.tekstowo.pl/piosenki_artysty,bajm,alfabetycznie,strona,5.html"],
                       "pm": ["http://www.tekstowo.pl/piosenki_artysty,patrycja_markowska.html",
                              "http://www.tekstowo.pl/piosenki_artysty,patrycja_markowska,alfabetycznie,strona,2.html",
                              "http://www.tekstowo.pl/piosenki_artysty,patrycja_markowska,alfabetycznie,strona,3.html"],
                       "eg": ["http://www.tekstowo.pl/piosenki_artysty,edyta_gorniak.html",
                              "http://www.tekstowo.pl/piosenki_artysty,edyta_gorniak,alfabetycznie,strona,2.html",
                              "http://www.tekstowo.pl/piosenki_artysty,edyta_gorniak,alfabetycznie,strona,3.html"],
                       "on": ["http://www.tekstowo.pl/piosenki_artysty,o_n_a_.html",
                              "http://www.tekstowo.pl/piosenki_artysty,o_n_a_,alfabetycznie,strona,2.html"],
                       "ap": ["http://www.tekstowo.pl/piosenki_artysty,andrzej_piaseczny.html",
                              "http://www.tekstowo.pl/piosenki_artysty,andrzej_piaseczny,alfabetycznie,strona,2.html",
                              "http://www.tekstowo.pl/piosenki_artysty,andrzej_piaseczny,alfabetycznie,strona,3.html"],
                       "nk": ["http://www.tekstowo.pl/piosenki_artysty,natalia_kukulska.html",
                              "http://www.tekstowo.pl/piosenki_artysty,natalia_kukulska,alfabetycznie,strona,2.html",
                              "http://www.tekstowo.pl/piosenki_artysty,natalia_kukulska,alfabetycznie,strona,3.html"],
                       "it": ["http://www.tekstowo.pl/piosenki_artysty,ich_troje.html",
                              "http://www.tekstowo.pl/piosenki_artysty,ich_troje,alfabetycznie,strona,2.html",
                              "http://www.tekstowo.pl/piosenki_artysty,ich_troje,alfabetycznie,strona,3.html",
                              "http://www.tekstowo.pl/piosenki_artysty,ich_troje,alfabetycznie,strona,3.html"]];

        def seeds = seeds00;

        seeds.each { id, urls ->
            def dir = "${outDir}/${id}";
            if (!new File(dir).exists()) {
                new File(dir).mkdirs();
            }
            def i = 0;
            urls.each { url ->
                try {
                    Document doc = Jsoup.connect(url).get();
                    def txtUrls = doc.select("div.box-przeboje").select("a[href~=piosenka]");
                    txtUrls.each { txtUrl ->
                        try {
                            Document txtDoc = Jsoup.connect(txtUrl.attr("abs:href")).get();
                            List<TextNode> txtNodes = txtDoc.select("div.song-text").get(0).textNodes();
                            def txt = txtNodes.collect { it.text() }.join("\n");

                            new File("${dir}/${String.format("%03d.txt", i)}").withWriter { f ->
                                f.write(txt);
                            }
                            i++;
                        } catch (Exception e) {
                            log.info("Exception at {}", txtUrl.attr("abs:href"), e)
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception at {}", url, e);
                }


            }

        }
    }

}
