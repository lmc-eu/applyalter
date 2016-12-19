package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.DigestException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

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
    public Alters loadAlters(String... filenames) {
        List<Alter> a = new ArrayList<Alter>(filenames.length);
        MessageDigest sourceDigest = initHashDigest();
        for (String f : filenames) {

            final boolean isZip;
            if (f.endsWith(ApplyAlter.XML_SUFFIX)) {
                isZip = false;
            } else if (f.endsWith(ApplyAlter.ZIP_SUFFIX)) {
                isZip = true;
            } else {
                throw new ApplyAlterException("Unknown filetype " + f);
            }

            if (f.contains("://")) {
                final URL url;
                try {
                    url = new URL(f);
                } catch (MalformedURLException e) {
                    throw new ApplyAlterException("invalid URL: " + f, e);
                }
                a.addAll(loadUrl(url, isZip, sourceDigest));
            } else {
                //file
                final File file = new File(f);
                a.addAll(loadFile(file, isZip, sourceDigest));
            }
        }
        return new Alters(a, bytes2hex(sourceDigest.digest()));
    }

    /**
     * Parse script file, validate it and load datafiles (if any).
     * This method sets {@link ch.ips.g2.applyalter.Alter#getId()} and {@link ch.ips.g2.applyalter.Alter#getHash()}
     * (note: this is the place where hash algorithm is implemented).
     */
    public Alter parseScriptFile(String fileId, AlterSource source, @Nullable MessageDigest extraDigest) {
        Alter alterscript;
        final MessageDigest digest = initHashDigest();

        InputStream inputStream = null;
        try {
            if (validator != null) {
                validator.validate(new StreamSource(source.openScript()));
            }
            inputStream = source.openScript();
            //compute hash on-the-fly
            inputStream = wrapDigesting(inputStream, digest);
            inputStream = wrapDigesting(inputStream, extraDigest);
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
                    inputStream = wrapDigesting(inputStream, digest);
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
     * Utility method: wrap stream in {@link DigestInputStream}, but not when the digest is null.
     *
     * @param inputStream input stream to wrap
     * @param digest      digest buffer to compute
     * @return digesting stream wrapper, or just the original stream
     */
    private static InputStream wrapDigesting(@Nonnull InputStream inputStream, @Nullable MessageDigest digest) {
        if (digest == null) {
            return inputStream;
        }
        return new DigestInputStream(inputStream, digest);
    }

    /**
     * Utility method: compute full digest of stream, consuming it.
     *
     * @param inputStream input stream to compute digest
     * @param digest      digest buffer to compute
     * @return the digest
     */
    private static MessageDigest digestAndClose(@Nonnull InputStream inputStream, @Nonnull MessageDigest digest)
            throws IOException, DigestException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) >= 0) {
            digest.update(buf, 0, len);
        }
        inputStream.close();
        return digest;
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
     * @param file         XML serialized Alter
     * @param sourceDigest
     * @return new Alter instance
     * @throws ch.ips.g2.applyalter.ApplyAlterException if file can not be found
     */
    private Collection<? extends Alter> loadFile(File file, boolean zip, MessageDigest sourceDigest) {
        if (!file.exists())
            throw new ApplyAlterException("file does not exist: " + file);

        if (zip) {
            return loadZip(file, sourceDigest);
        } else {
            final AlterSource source = new FileSource(file);
            return Collections.singletonList(parseScriptFile(file.toString(), source, sourceDigest));
        }

    }

    private Collection<? extends Alter> loadUrl(URL url, boolean zip, MessageDigest sourceDigest) {
        if (zip) {
            return loadZip(url, sourceDigest);
        } else {
            final AlterSource source = new UrlSource(url);
            return Collections.singletonList(parseScriptFile(url.toString(), source, sourceDigest));
        }
    }


    /**
     * Create a list of Alter instances from XML serialized from files stored in .zip.
     * List is sorted using {@link ch.ips.g2.applyalter.ZipEntryNameComparator}.
     *
     * @param zipfile      zip file containing XML files
     * @param sourceDigest source digest
     * @return list of new Alter instances
     * @throws ch.ips.g2.applyalter.ApplyAlterException if error occurs during zip file processing
     */
    protected List<Alter> loadZip(File zipfile, MessageDigest sourceDigest) {
        try {
            return loadZip(zipfile.toURI().toURL(), sourceDigest);
        } catch (IOException e) {
            throw new ApplyAlterException("Error reading zip file " + zipfile, e);
        }
    }

    /**
     * Create a list of Alter instances from XML serialized from files stored in .zip.
     * List is sorted using {@link ch.ips.g2.applyalter.ZipEntryNameComparator}.
     * <p />
     * Implementation note: the input zip is actually read several times.
     *
     * @param zipfile zip file containing XML files
     * @return list of new Alter instances
     * @throws ch.ips.g2.applyalter.ApplyAlterException if error occurs during zip file processing
     */
    protected List<Alter> loadZip(URL zipfile, @Nonnull MessageDigest sourceDigest) {
        final List<String> alterNames;
        try {
            InputStream inputStream = zipfile.openStream();
            final ZipInputStream zis = new ZipInputStream(inputStream);
            alterNames = new ArrayList<String>();

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                final String entryName = entry.getName();
                if (entryName.endsWith(ApplyAlter.XML_SUFFIX)) {
                    alterNames.add(entryName);
                }
            }
            zis.close();

            digestAndClose(zipfile.openStream(), sourceDigest);
        } catch (IOException e) {
            throw new ApplyAlterException("Error reading zip file " + zipfile, e);
        } catch (DigestException e) {
            throw new ApplyAlterException("Error computing digest of " + zipfile, e);
        }

        List<Alter> a = new ArrayList<Alter>(alterNames.size());
        Collections.sort(alterNames);

        for (String alterName : alterNames) {
            AlterSource source = new JarUrlSource(zipfile, alterName);
            //note: this.sourceDigest already contains whole zip, do NOT update it with alterscript content!
            Alter alterscript = parseScriptFile(alterName, source, null);
            a.add(alterscript);
        }

        return a;
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
     * Alterscript loaded from url, data files are in the same directory.
     * <br />
     * Warning: this implementation supports only non-opaque URLs.
     * In particular, urls with jar: schema do not work correctly.
     */
    public static class UrlSource implements AlterSource {
        private URL url;

        public UrlSource(URL url) {
            this.url = url;
        }

        public InputStream openScript()
                throws IOException {
            return url.openStream();
        }

        public InputStream openDataFile(String filename)
                throws IOException {
            try {
                URI relative = url.toURI().resolve(filename);
                return relative.toURL().openStream();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * Alterscript loaded from inside of JAR (which is referenced by URL).
     */
    public static class JarUrlSource implements AlterSource {
        private final URL jarUrl;
        /**
         * Entry path: always absolute!
         */
        private final URI entryPath;

        public JarUrlSource(URL jarUrl, String path) {
            this.jarUrl = jarUrl;
            this.entryPath = URI.create(path.startsWith("/") ? path : "/" + path);
        }

        public URI getEntryPath() {
            return entryPath;
        }

        public InputStream openScript()
                throws IOException {
            return makeURL(entryPath).openStream();
        }

        /**
         * Compose JAR url (made from jar file url, exclamation point and internal path).
         *
         * @param entryPath entry path (must be absolute)
         * @return final URL
         */
        private URL makeURL(URI entryPath) throws MalformedURLException {
            assert entryPath.isAbsolute();
            return new URL("jar:" + jarUrl + "!" + entryPath);
        }

        public InputStream openDataFile(String filename)
                throws IOException {
            //note: resolution must be done on the internal part, without the external zipfile path!
            URI dataEntryPath = getEntryPath().resolve(filename);
            return makeURL(dataEntryPath).openStream();
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
