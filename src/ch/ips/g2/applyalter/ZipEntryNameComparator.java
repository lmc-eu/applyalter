package ch.ips.g2.applyalter;
import java.util.Comparator;
import java.util.zip.ZipEntry;

/**
 * Alphabeticaly compares {@link ZipEntry ZipEntries} according to their {@link ZipEntry#getName()}
 */
public class ZipEntryNameComparator implements
    Comparator<ZipEntry>
{
  public int compare(ZipEntry i, ZipEntry j) {
    if (i.equals(j))
      return 0;
    int res = i.getName().compareTo(j.getName());
    if (res != 0)
      return res;
    return i.toString().compareTo(j.toString());
  }
}