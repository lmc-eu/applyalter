package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AlterLoader {
    /**
     * XStream deserializer, correctly initialized.
     */
    protected final XStream xstream;
    /**
     * XML validator
     */
    protected final Validator validator;

    public AlterLoader(XStream xstream, Validator validator) {
        this.xstream = xstream;
        this.validator = validator;
    }

    /**
     * Load alterscripts from files and ZIP archives.
     */
    public List<Alter> loadAlters(String... filenames) {
        List<Alter> a = new ArrayList<Alter>(filenames.length);
        for (String f : filenames) {
            if (f.endsWith(ApplyAlter.XML_SUFFIX)) {
                a.add(fromFile(f));
            } else if (f.endsWith(ApplyAlter.ZIP_SUFFIX)) {
                a.addAll(fromZip(f));
            } else {
                throw new ApplyAlterException("Unknown filetype " + f);
            }
        }
        return a;
    }

    /**
     * Parse script file, validate it and load datafiles (if any).
     * This method sets {@link ch.ips.g2.applyalter.Alter#getId()} and {@link ch.ips.g2.applyalter.Alter#getHash()}
     * (note: this is the place where hash algorithm is implemented).
     */
    public Alter parseScriptFile(String fileId, AlterSource source) {
        Alter alterscript;
        final MessageDigest digest = initHashDigest();

        InputStream inputStream = null;
        try {
            if (validator != null) {
                validator.validate(new StreamSource(source.openScript()));
            }
            inputStream = source.openScript();
            //compute hash on-the-fly
            inputStream = new DigestInputStream(inputStream, digest);
            alterscript = (Alter) xstream.fromXML(inputStream);
        } catch (XStreamException e) {
            throw new ApplyAlterException("Unable to deserialize Alter from file " + fileId, e);
        } catch (FileNotFoundException e) {
            throw new ApplyAlterException("File not found " + fileId, e);
        } catch (SAXException e) {
            throw new ApplyAlterException("Can not validate file " + fileId, e);
        } catch (IOException e) {
            throw new ApplyAlterException("I/O exception during XML file validation " + fileId, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
            //BaseUtil.closeNoThrow( inputStream, "parseScriptFile" );
        }

        //load data files
        if (alterscript.datafile != null && alterscript.datafile.size() > 0) {
            alterscript._datafiles = new TreeMap<String, byte[]>();
            for (String datafile : alterscript.datafile) {
                inputStream = null;
                try {
                    inputStream = source.openDataFile(datafile);
                    inputStream = new DigestInputStream(inputStream, digest);
                    byte[] data = IOUtils.toByteArray(inputStream);
                    alterscript._datafiles.put(datafile, data);
                } catch (FileNotFoundException e) {
                    throw new ApplyAlterException("no such datafile: " + datafile + " for " + fileId, e);
                } catch (IOException e) {
                    throw new ApplyAlterException("error reading datafile: " + datafile + " for " + fileId, e);
                } finally {
                    IOUtils.closeQuietly(inputStream);
//          BaseUtil.closeNoThrow( inputStream, "parseScriptFile" );
                }
            }
        }

        // set filename part
        alterscript.setId(new File(fileId).getName());
        //set hash
        final byte[] hashBytes = digest.digest();
        alterscript.setHash(bytes2hex(hashBytes));

        return alterscript;
    }

    /**
     * Convert bytes to hex string.
     */
    protected static String bytes2hex(byte[] hashBytes) {
        char[] hexChars = new char[2 * hashBytes.length];
        int ci = 0;
        for (byte hashByte : hashBytes) {
            //trick: & 0xFF makes signed value unsigned
            int b = hashByte & 0xff;
            hexChars[ci++] = Character.toUpperCase(Character.forDigit(b >> 4, 16));
            hexChars[ci++] = Character.toUpperCase(Character.forDigit(b & 0xf, 16));
        }
        return new String(hexChars);
    }

    /**
     * Initialize hash algorithm.
     *
     * @return hash digest
     */
    private MessageDigest initHashDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new ApplyAlterException("Hash algorithm not supported", e);
        }
    }

    /**
     * Create Alter instance from XML serialized from file
     *
     * @param file XML serialized Alter
     * @return new Alter instance
     * @throws ch.ips.g2.applyalter.ApplyAlterException
     *          if file can not be found
     */
    protected Alter fromFile(String file)
            throws ApplyAlterException {
        AlterSource source = new FileSource(new File(file));
        return parseScriptFile(file, source);
    }

    /**
     * Create a list of Alter instances from XML serialized from files stored in .zip.
     * List is sorted using {@link ch.ips.g2.applyalter.ZipEntryNameComparator}.
     *
     * @param zipfile zip file containing XML files
     * @return list of new Alter instances
     * @throws ch.ips.g2.applyalter.ApplyAlterException
     *          if error occurs during zip file processing
     */
    protected List<Alter> fromZip(String zipfile) {
        try {
            final ZipFile z = new ZipFile(zipfile);
            List<ZipEntry> alterEntries = new ArrayList<ZipEntry>();
            final Map<String, ZipEntry> allFiles = new HashMap<String, ZipEntry>();

            Enumeration<? extends ZipEntry> zipEntryEnum = z.entries();
            while (zipEntryEnum.hasMoreElements()) {
                ZipEntry zipFile = zipEntryEnum.nextElement();
                if (zipFile.isDirectory()) {
                    continue;
                }
                allFiles.put(zipFile.getName(), zipFile);
                if (zipFile.getName().endsWith(ApplyAlter.XML_SUFFIX)) {
                    alterEntries.add(zipFile);
                }
            }

            List<Alter> a = new ArrayList<Alter>(alterEntries.size());
            Collections.sort(alterEntries, new ZipEntryNameComparator());
            for (final ZipEntry entry : alterEntries) {
                AlterSource source = new ZipAlterSource(z, entry, allFiles);

                Alter alterscript = parseScriptFile(entry.getName(), source);
                a.add(alterscript);
            }

            return a;
        } catch (IOException e) {
            throw new ApplyAlterException("Error reading zip file " + zipfile, e);
        }
    }

    //-----------------------------------------------------------------------------------------------------------------

    /**
     * Generic alterscript source: either files on fielsystem or files in archive.
     * All alterscripts must be able to be loaded several times!
     */
    public interface AlterSource {
        InputStream openScript()
                throws IOException;

        /**
         * Open data file (from the same directory/archive).
         *
         * @param filename filename (relative)
         * @return data to read
         * @throws FileNotFoundException no such file
         * @throws IOException           error opening
         */
        InputStream openDataFile(String filename)
                throws IOException;
    }

    /**
     * Alterscript loaded from file, data files are in the same directory.
     */
    public static class FileSource implements AlterSource {
        private File file;

        public FileSource(File file) {
            this.file = file;
        }

        public InputStream openScript()
                throws IOException {
            return new FileInputStream(file);
        }

        public InputStream openDataFile(String filename)
                throws IOException {
            File dataFile = new File(file.getParentFile(), filename);
            return new FileInputStream(dataFile);
        }
    }

    /**
     * Alterscript loaded ZIP archive, data files are in the same archive (note: only root level is supported).
     */
    public static class ZipAlterSource implements AlterSource {
        private final ZipFile zip;
        private final ZipEntry scriptEntry;
        private final Map<String, ZipEntry> allFiles;

        public ZipAlterSource(ZipFile zip, ZipEntry scriptEntry, Map<String, ZipEntry> allFiles) {
            this.zip = zip;
            this.scriptEntry = scriptEntry;
            this.allFiles = allFiles;
        }

        public InputStream openScript()
                throws IOException {
            return zip.getInputStream(scriptEntry);
        }

        public InputStream openDataFile(String filename)
                throws IOException {
            ZipEntry dataEntry = allFiles.get(filename);
            if (dataEntry == null)
                throw new FileNotFoundException("no such file in archive: " + filename + " in " + zip.getName());
            return zip.getInputStream(dataEntry);
        }
    }

    /**
     * Alterscript loaded from classpath (relative to some class), data files are in the same package.
     */
    public static class RelativeToClassAlterSource implements AlterSource {
        private final Class<?> baseClass;
        private final String alterName;

        public RelativeToClassAlterSource(Class<?> baseClass, String alterName) {
            this.baseClass = baseClass;
            this.alterName = alterName;
        }

        public InputStream openScript()
                throws IOException {
            return baseClass.getResourceAsStream(alterName);
        }

        public InputStream openDataFile(String filename)
                throws IOException {
            return baseClass.getResourceAsStream(filename);
        }
    }
}
