package ch.ips.g2.applyalter;

import java.io.Serializable;
import java.util.Comparator;
import java.util.zip.ZipEntry;

/**
 * Alphabetically compares {@link ZipEntry ZipEntries} according to their {@link ZipEntry#getName()}
 *
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ZipEntryNameComparator implements
        Comparator<ZipEntry>, Serializable {
    public int compare(ZipEntry i, ZipEntry j) {
        if (i.equals(j))
            return 0;
        int res = i.getName().compareTo(j.getName());
        if (res != 0)
            return res;
        return i.toString().compareTo(j.toString());
    }
}