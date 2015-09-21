package pl.net.lkd.clamber.impl

import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.StringEscapeUtils
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.NodeChild
import groovyx.net.http.HTTPBuilder


@Slf4j
class EpprocCrawler {

    private static String PATH_TO_FILE
    private static String PATH_TO_JSON

    private class JsonData {
        def xmlid
        def meta_text_xmlid
        def title_m
        def title_a
        def pub_date
        def pub_date_raw
        def acquisition_date
        def modified_date
        def lang
        def word_count
        def url
        def chapter
    }

    def listOfFiles = [File]
    def listOfDates = []
    def listOfFileNames = []
    def listOfDatesMap = [:]
    def listOfRecords = []
    def urlPl
    def urlEn

    public EpprocCrawler(rootDir, fout) {
        PATH_TO_FILE = rootDir
        PATH_TO_JSON = fout
    }

    def buildJsonFile(list) {
        def jsonFile = new JsonBuilder()
        if (list.size() != 0) {

            jsonFile {
                records(
                      list.collect {
                          JsonData a -> [xmlid: a.xmlid, meta_text_xmlid: a.meta_text_xmlid, pub_date: a.pub_date, lang: a.lang, url: urlPl, title_a: a.title_a, word_count: a.word_count, original_file: a.chapter]
                      }
                )
            }
        }
        return StringEscapeUtils.unescapeJavaScript(jsonFile.toPrettyString())
    }

    def createDate(f) {
        String[] fName = f.name.split("-");
        StringBuilder st = new StringBuilder()

        if (fName.length > 4) {

            Integer year = Integer.valueOf(fName[1])
            Integer month = Integer.valueOf(fName[2])
            Integer day = Integer.valueOf(fName[3])

            if (year > 14) {
                st.append("19")
                st.append(year)
            } else {
                st.append("20")
                if (year < 10) {
                    st.append("0")
                    st.append(year)
                } else {
                    st.append(year)
                }
            }

            if (month < 10) {
                st.append("0")
                st.append(month)
            } else {
                st.append(month)
            }

            if (day < 10) {
                st.append("0")
                st.append(day)
            } else {
                st.append(day)
            }
        }
        return st.toString()
    }

    def filingListByFiles(List files) {
        File dir = new File(PATH_TO_FILE)
        dir.eachFileRecurse(FileType.FILES) { file ->
            files << file
        }
    }

    def fillingListByDates() {
        listOfFiles.collect { elem ->
            String result = createDate(elem)
            if (!result.isEmpty()) {
                listOfDates << result
                listOfFileNames << elem.name
            }
        }
        for (int i = 0; i < listOfDates.size(); i++) {
            listOfDatesMap.put(listOfDates[i], listOfFileNames[i])
        }
    }


    def extractChapter(str) {
        StringBuilder result = new StringBuilder()
        String[] items = str.trim().split(' ')
        // println items[0]
        String[] elements = items[0].toString().split("\\.")
        if (elements.length == 1) {
            if (elements[0].length() == 1) {
                result.append('00')
                result.append(elements[0])
            }

            if (elements[0].length() == 2) {
                result.append('0')
                result.append(elements[0])
            }
        }



        if (elements.length == 2) {

            if (elements[0].length() == 1) {
                result.append('00')
                result.append(elements[0])
            }

            if (elements[0].length() == 2) {
                result.append('0')
                result.append(elements[0])
            }

            result.append('-')

            if (elements[1].length() == 1) {
                result.append('0')
                result.append(elements[1])
            }

            if (elements[1].length() == 2) {
                result.append(elements[1])
            }
        }
        return result.toString()
    }

    def createNameFile(chapter, date) {
        String[] currentDate = date.trim().split('-')
        StringBuilder result = new StringBuilder()
        result.append('ep-')
        String d = currentDate[2].substring(2, currentDate[2].length())
        result.append(d)
        result.append('-')
        result.append(currentDate[1])
        result.append('-')
        result.append(currentDate[0])
        result.append('-')
        result.append(chapter)
        result.append('.xml')
        return result.toString()
    }

    def buildingLink(date, lang) {
        StringBuilder link = new StringBuilder()
        switch (lang) {
            case 'EN':
                link.append("http://www.europarl.europa.eu/sides/getDoc.do?pubRef=-//EP//TEXT+CRE+")
                link.append(date)
                link.append("+ITEMS+DOC+XML+V0//EN&language=EN")
                return link.toString()
            case 'PL':
                link.append("http://www.europarl.europa.eu/sides/getDoc.do?pubRef=-//EP//TEXT+CRE+")
                link.append(date)
                link.append("+ITEMS+DOC+XML+V0//PL&language=PL")
                return link.toString()
            default:
                link.append("http://www.europarl.europa.eu/sides/getDoc.do?pubRef=-//EP//TEXT+CRE+")
                link.append(date)
                link.append("+ITEMS+DOC+XML+V0//EN&language=EN")
                return link.toString()
        }
    }

    def extractData(NodeChild childEn, NodeChild childPl) {
        String xmlid
        String meta_text_xmlid
        String pub_date
        String lang

        def recordsPl = []
        def recordsEn = []

        int rozdzial = 0
        int chapter = 0
        int iteration = 0
        int iterationEn = 0
        childPl.'**'.findAll {

            def record = new JsonData()

            //title
            if (it.name() == "HEAD") {
                String tmp = it.toString()
                tmp = tmp.replace('-', '').replace(',', ' ').replace('.', '')
                StringBuilder result = new StringBuilder()


                meta_text_xmlid = tmp.replace(' ', '_').replaceAll('__', '_')
                result.append(meta_text_xmlid.replaceAll('_', '').replaceAll(' ', ''))
                result.append('|')
                result.append(meta_text_xmlid)

                xmlid = result.toString().trim()
            }
            record.meta_text_xmlid = meta_text_xmlid
            record.xmlid = xmlid

            //word count, chapter
            if (it.name() == 'TABLE' && it.@class == 'doc_box_header') {
                iteration++
                String content = it.childNodes()[0].childNodes()[0].childNodes()[0].text()
                int wordCount = content.collect {
                    it.charAt(0).digit || it.charAt(0).letter ? it : ' '
                }.join('').tokenize(' ').size()



                if (iteration > 1) {
                    record.word_count = wordCount

                    rozdzial++
                    record.title_a = 'Rozdział ' + rozdzial
                }

                NodeChild current = it

                current.'**'.findAll {
                    if (it.name() == 'TD' && it.@class == 'doc_title' && it.@style == 'background-image:url(/img/struct/navigation/gradient_blue.gif)') {
                        record.chapter = createNameFile(extractChapter(it.toString()), pub_date)
                    }
                }
            }

            //publication data
            if (it.name() == 'META') {
                if (it.@name == 'available') {
                    pub_date = it.@content
                }

                if (it.@name == 'language') {
                    lang = it.@content
                }
            }
            record.pub_date = pub_date
            record.lang = lang

            if (record.word_count != null) {
                recordsPl.add(record)
            }
        }

        childEn.'**'.findAll {
            def record = new JsonData()
            record.xmlid = xmlid
            record.meta_text_xmlid = meta_text_xmlid

            //word count, chapter
            if (it.name() == 'TABLE' && it.@class == 'doc_box_header') {
                iterationEn++
                String content = it.childNodes()[0].childNodes()[0].childNodes()[0].text()
                int wordCount = content.collect {
                    it.charAt(0).digit || it.charAt(0).letter ? it : ' '
                }.join('').tokenize(' ').size()

                if (iterationEn > 1) {
                    record.word_count = wordCount

                    chapter++
                    record.title_a = 'Chapter ' + chapter
                }
                NodeChild current = it

                current.'**'.findAll {
                    if (it.name() == 'TD' && it.@class == 'doc_title' && it.@style == 'background-image:url(/img/struct/navigation/gradient_blue.gif)') {

                        record.chapter = createNameFile(extractChapter(it.toString()), pub_date)
                    }
                }
            }
            //publication data
            if (it.name() == 'META') {
                if (it.@name == 'available') {
                    pub_date = it.@content
                }

                if (it.@name == 'language') {
                    lang = it.@content
                }
            }
            record.pub_date = pub_date
            record.lang = lang

            if (record != null & record.word_count != null) {
                recordsEn.add(record)
            }
        }

        listOfRecords = []
        for (int i = 0; i < recordsPl.size(); i++) {
            listOfRecords.add(recordsPl[i])
            listOfRecords.add(recordsEn[i])
        }
    }

    def callWebSite() {
        listOfDatesMap.eachWithIndex { key, value, idx ->
            log.info("Processing item {} [${idx + 1}/${listOfDatesMap.size()}].", value)
            String linkEn = buildingLink(key, 'EN')
            String linkPl = buildingLink(key, 'PL')
            urlPl = linkPl
            urlEn = linkEn

            HTTPBuilder httpBuilderEn = new HTTPBuilder(linkEn)
            def htmlEn = httpBuilderEn.get([:])

            HTTPBuilder httpBuilderPl = new HTTPBuilder(linkPl)
            def htmlPl = httpBuilderPl.get([:])

            extractData(htmlEn, htmlPl)
            String result = buildJsonFile(listOfRecords)
            File json = new File(PATH_TO_JSON, "${key}.txt")

            json.withWriter('UTF-8') {
                it << result
            }

            listOfRecords = []
            //Remove if you want to load all data
            //break
        }
    }

    def init() {
        filingListByFiles(listOfFiles)
        fillingListByDates()
        callWebSite()
    }

    public static void main(String[] args) {
        def rootDir = "/mnt/ssh/data0/CLARIN/A7/data/src/epproc/raw/"
        def fout = "/tmp/epproc"
        EpprocCrawler searchFile = new EpprocCrawler(rootDir, fout)
        searchFile.init()
    }

}