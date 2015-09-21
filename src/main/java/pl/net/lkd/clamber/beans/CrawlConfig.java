package pl.net.lkd.clamber.beans;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pl.net.lkd.clamber.writers.CSVWriter;
import pl.net.lkd.clamber.writers.Writer;

import java.util.regex.Pattern;

@Slf4j
@Data
public class CrawlConfig {
    String startURL = null;
    String outFile = null;

    boolean debug = false;
    int limit = Integer.MAX_VALUE;
    int timeout = 3000;
    Pattern mimetypesToVisit = Pattern.compile(".*(text/*|text/html|application/xml|application/xhtml\\+xml).*");
    Pattern mimetypesToRecord = Pattern.compile(".*(text/*|text/html|application/xml|application/xhtml\\+xml).*");
    Pattern patternsToVisit = Pattern.compile(".*");
    Pattern patternsToRecord = Pattern.compile(".*");
    Writer writer = new CSVWriter();
}