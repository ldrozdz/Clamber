package pl.net.lkd.clamber.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Link {
  String parent;
  String name;
  String href;
  String mimetype;
}