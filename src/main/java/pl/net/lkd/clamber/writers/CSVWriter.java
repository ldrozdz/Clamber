package pl.net.lkd.clamber.writers;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import pl.net.lkd.clamber.beans.CrawlConfig;
import pl.net.lkd.clamber.beans.Link;

import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class CSVWriter extends Writer {

  FileWriter writer;

  public void init(CrawlConfig _config) {
    config = _config;
    try {
      writer = new FileWriter(config.getOutFile());
    } catch (IOException e) {
      log.error("{}", e);
    }
  }

  public void close() {
    if (writer != null) {
      try {
        writer.close();
      } catch (IOException e) {
        log.error("{}", e);
      }
    }
  }

  @Override
  public void writeRecord(Link l) {
    if (writer != null &&
        config.getMimetypesToRecord().matcher(l.getMimetype()).matches() &&
        config.getPatternsToRecord().matcher(l.getHref()).matches()) {
      try {
        String line = String.format("\"%s\",\"%s\",\"%s\"\n",
            l.getParent().trim().replaceAll("\"","\"\""),
            l.getName().trim().replaceAll("\"","\"\""),
            l.getHref().trim().replaceAll("\"","\"\""));
        writer.write(line);
        writer.flush();
      } catch (IOException e) {
        log.error("{}", e);
      }
    }
  }

}
