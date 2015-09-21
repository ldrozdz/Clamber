package pl.net.lkd.clamber.writers;

import pl.net.lkd.clamber.beans.CrawlConfig;
import pl.net.lkd.clamber.beans.Link;

import java.io.FileWriter;
import java.io.IOException;

public abstract class Writer {
  CrawlConfig config;

  public abstract void init(CrawlConfig _config);
  public abstract void writeRecord(Link l);
  public abstract void close();
}
