package pl.net.lkd.clamber.impl

import groovy.util.logging.Slf4j
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode

@Slf4j
class LyricsWikiaCrawler {
    public static void main(String[] args) {
        def outDir = "/tmp/us00";
        def seeds60 = ["or": "http://lyrics.wikia.com/Otis_Redding",
                       "ep": "http://lyrics.wikia.com/Elvis_Presley",
                       "ro": "http://lyrics.wikia.com/Roy_Orbison",
                       "pa": "http://lyrics.wikia.com/Paul_Anka",
                       "eb": "http://lyrics.wikia.com/The_Everly_Brothers",
                       "bb": "http://lyrics.wikia.com/The_Beach_Boys",
                       "pp": "http://lyrics.wikia.com/Peter,_Paul_%26_Mary",
                       "mp": "http://lyrics.wikia.com/The_Mamas_%26_The_Papas",
                       "sl": "http://lyrics.wikia.com/The_Shangri-Las",
                       "sc": "http://lyrics.wikia.com/Sonny_%26_Cher"];

        def seeds00 = ["bb": "http://lyrics.wikia.com/Backstreet_Boys",
                       "bs": "http://lyrics.wikia.com/Britney_Spears",
                       "ca": "http://lyrics.wikia.com/Christina_Aguilera",
                       "be": "http://lyrics.wikia.com/Beyonc%C3%A9",
                       "jl": "http://lyrics.wikia.com/Jennifer_Lopez",
                       "bb": "http://lyrics.wikia.com/The_Beach_Boys",
                       "nb": "http://lyrics.wikia.com/Nickelback",
                       "sk": "http://lyrics.wikia.com/Shakira",
                       "ri": "http://lyrics.wikia.com/Rihanna",
                       "jt": "http://lyrics.wikia.com/Justin_Timberlake"];

        def seeds = seeds00;

        seeds.each { id, url ->
            def dir = "${outDir}/${id}";
            if (!new File(dir).exists()) {
                new File(dir).mkdirs();
            }
            def i = 0;
            try {
                Document doc = Jsoup.connect(url).get();
                def txtUrls = doc.select("li>b>a");
                txtUrls.each { txtUrl ->
                    if (i < 100) {
                        try {
                            Document txtDoc = Jsoup.connect(txtUrl.attr("abs:href")).get();
                            List<TextNode> txtNodes = txtDoc.select("div.lyricbox").get(0).textNodes();
                            def txt = txtNodes.collect { it.text() }.join("\n");
                            new File("${dir}/${String.format("%03d.txt", i)}").withWriter { f ->
                                f.write(txt);
                            }
                            i++;
                        } catch (Exception e) {
                            log.info("Exception at {}", txtUrl.attr("abs:href"), e)
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Exception at {}", url, e);
            }

        }
    }
}
