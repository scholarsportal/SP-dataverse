/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.ingest.IngestReport;
import edu.harvard.iq.dataverse.ingest.IngestUtil;
import edu.harvard.iq.dataverse.ingest.IngestServiceShapefileHelper;
import edu.harvard.iq.dataverse.ingest.IngestableDataChecker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.MimetypesFileTypeMap;
import javax.ejb.EJBException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * a 4.0 implementation of the DVN FileUtil;
 * it provides some of the functionality from the 3.6 implementation, 
 * but the old code is ported creatively on the method-by-method basis.
 * 
 * @author Leonid Andreev
 */
public class FileUtil implements java.io.Serializable  {
    private static final Logger logger = Logger.getLogger(FileUtil.class.getCanonicalName());
    
    private static final String[] TABULAR_DATA_FORMAT_SET = {"POR", "SAV", "DTA", "RDA"};
    
    private static Map<String, String> STATISTICAL_FILE_EXTENSION = new HashMap<String, String>();
   
    /*
     * The following are Stata, SAS and SPSS syntax/control cards: 
     * These are recognized as text files (because they are!) so 
     * we check all the uploaded "text/plain" files for these extensions, and 
     * assign the following types when they are matched;
     * Note that these types are only used in the metadata displayed on the 
     * dataset page. We don't support ingest on control cards. 
     * -- L.A. 4.0 Oct. 2014
    */
    
    static {
        STATISTICAL_FILE_EXTENSION.put("do",  "application/x-stata-syntax");
        STATISTICAL_FILE_EXTENSION.put("sas", "application/x-sas-syntax");
        STATISTICAL_FILE_EXTENSION.put("sps", "application/x-spss-syntax");
        STATISTICAL_FILE_EXTENSION.put("csv", "text/csv");
    }
    
    private static MimetypesFileTypeMap MIME_TYPE_MAP = new MimetypesFileTypeMap();
    
    public static final String MIME_TYPE_STATA = "application/x-stata";
    public static final String MIME_TYPE_STATA13 = "application/x-stata-13";
    public static final String MIME_TYPE_RDATA = "application/x-rlang-transport";
    
    public static final String MIME_TYPE_CSV   = "text/csv";
    public static final String MIME_TYPE_CSV_ALT = "text/comma-separated-values";
    
    public static final String MIME_TYPE_XLSX  = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String MIME_TYPE_SPSS_SAV = "application/x-spss-sav";
    public static final String MIME_TYPE_SPSS_POR = "application/x-spss-por";
    
    public static final String MIME_TYPE_TAB   = "text/tab-separated-values";
    
    public static final String MIME_TYPE_FITS  = "application/fits";
    
    public static final String MIME_TYPE_ZIP   = "application/zip";
    
    public static final String MIME_TYPE_UNDETERMINED_DEFAULT = "application/octet-stream";
    public static final String MIME_TYPE_UNDETERMINED_BINARY = "application/binary";
    
    public static final String SAVED_ORIGINAL_FILENAME_EXTENSION = "orig";
    
    public FileUtil() {
    }
    
    public static void copyFile(File inputFile, File outputFile) throws IOException {
        FileChannel in = null;
        WritableByteChannel out = null;
        
        try {
            in = new FileInputStream(inputFile).getChannel();
            out = new FileOutputStream(outputFile).getChannel();
            long bytesPerIteration = 50000;
            long start = 0;
            while ( start < in.size() ) {
                in.transferTo(start, bytesPerIteration, out);
                start += bytesPerIteration;
            }
            
        } finally {
            if (in != null) { in.close(); }
            if (out != null) { out.close(); }
        }
    }

   
    public static String getFileExtension(String fileName){
        String ext = null;
        if ( fileName.lastIndexOf(".") != -1){
            ext = (fileName.substring( fileName.lastIndexOf(".") + 1 )).toLowerCase();
        }
        return ext;
    } 

    public static String replaceExtension(String originalName) {
       return replaceExtension(originalName, "tab");
    }   
    
    public static String replaceExtension(String originalName, String newExtension) {
        int extensionIndex = originalName.lastIndexOf(".");
        if (extensionIndex != -1 ) {
            return originalName.substring(0, extensionIndex) + "."+newExtension ;
        } else {
            return originalName +"."+newExtension ;
        }
    }
    
    public static String getUserFriendlyFileType(DataFile dataFile) {
        String fileType = dataFile.getContentType();
         
        if (fileType != null) {
            if (fileType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)){
                return ShapefileHandler.SHAPEFILE_FILE_TYPE_FRIENDLY_NAME;
            }
            if (fileType.indexOf(";") != -1) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }
            try {
                return ResourceBundle.getBundle("MimeTypeDisplay").getString(fileType);
            } catch (MissingResourceException e) {
                return fileType;
            }
        }

        return fileType;
    }
    
    public static String getFacetFileType(DataFile dataFile) {
        String fileType = dataFile.getContentType();
        
        if (fileType != null) {
            if (fileType.indexOf(";") != -1) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }

            try {
                return ResourceBundle.getBundle("MimeTypeFacets").getString(fileType);
            } catch (MissingResourceException e) {
                // if there's no defined "facet-friendly" form of this mime type
                // we'll truncate the available type by "/", e.g., all the 
                // unknown image/* types will become "image"; many other, quite
                // different types will all become "application" this way - 
                // but it is probably still better than to tag them all as 
                // "uknown". 
                // -- L.A. 4.0 alpha 1
                return fileType.split("/")[0];
            }
        }
        
        return "unknown"; 
    }
    
    public static String getUserFriendlyOriginalType(DataFile dataFile) {
        String fileType = dataFile.getOriginalFileFormat();
         
        if (fileType != null && !fileType.equals("")) {
            if (fileType.indexOf(";") != -1) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }
            try {
                return ResourceBundle.getBundle("MimeTypeDisplay").getString(fileType);
            } catch (MissingResourceException e) {
                return fileType;
            }
        } 
        
        return "UNKNOWN";
    }
    
    /**
     *  Returns a content type string for a FileObject
     * 
     */
    private static String determineContentType(File fileObject) {
        if (fileObject==null){
            return null;
        }
        String contentType;
        try {
            contentType = determineFileType(fileObject, fileObject.getName());
        } catch (Exception ex) {
            logger.warning("FileUtil.determineFileType failed for file with name: " + fileObject.getName());
            contentType = null;
        }

       if ((contentType==null)||(contentType.equals(""))){
            contentType = MIME_TYPE_UNDETERMINED_DEFAULT;
       }
       return contentType;
        
    }
    
    public static String determineFileType(File f, String fileName) throws IOException{
        String fileType = null;
        String fileExtension = getFileExtension(fileName);

        
        
        // step 1: 
        // Apply our custom methods to try and recognize data files that can be 
        // converted to tabular data, or can be parsed for extra metadata 
        // (such as FITS).
        logger.fine("Attempting to identify potential tabular data files;");
        IngestableDataChecker tabChk = new IngestableDataChecker(TABULAR_DATA_FORMAT_SET);
        
        fileType = tabChk.detectTabularDataFormat(f);
        
        logger.fine("determineFileType: tabular data checker found "+fileType);
                
        // step 2: If not found, check if graphml or FITS
        if (fileType==null) {
            if (isGraphMLFile(f))  {
                fileType = "text/xml-graphml";
            } else // Check for FITS:
            // our check is fairly weak (it appears to be hard to really
            // really recognize a FITS file without reading the entire 
            // stream...), so in version 3.* we used to nsist on *both* 
            // the ".fits" extension and the header check;
            // in 4.0, we'll accept either the extension, or the valid 
            // magic header:
            if (isFITSFile(f) || (fileExtension != null
                    && fileExtension.equalsIgnoreCase("fits"))) {
                fileType = "application/fits";
            }
        }
       
        // step 3: check the mime type of this file with Jhove
        if (fileType == null){
            JhoveFileType jw = new JhoveFileType();
            fileType = jw.getFileMimeType(f);
        }
        
        // step 4: 
        // Additional processing; if we haven't gotten much useful information 
        // back from Jhove, we'll try and make an educated guess based on 
        // the file extension:
        
        if ( fileExtension != null) {
            logger.fine("fileExtension="+fileExtension);

            if (fileType == null || fileType.startsWith("text/plain") || "application/octet-stream".equals(fileType)) {
                if (fileType.startsWith("text/plain") && STATISTICAL_FILE_EXTENSION.containsKey(fileExtension)) {
                    fileType = STATISTICAL_FILE_EXTENSION.get(fileExtension);
                } else {
                    fileType = determineFileTypeByExtension(fileName);
                }
                
                logger.fine("mime type recognized by extension: "+fileType);
            }
        } else {
            logger.fine("fileExtension is null");
        }
        
        // step 5: 
        // if this is a compressed file - zip or gzip - we'll check the 
        // file(s) inside the compressed stream and see if it's one of our
        // recognized formats that we want to support compressed:

        if ("application/x-gzip".equals(fileType)) {
            logger.fine("we'll run additional checks on this gzipped file.");
            // We want to be able to support gzipped FITS files, same way as
            // if they were just regular FITS files:
            FileInputStream gzippedIn = new FileInputStream(f);
            // (new FileInputStream() can throw a "filen not found" exception;
            // however, if we've made it this far, it really means that the 
            // file does exist and can be opened)
            InputStream uncompressedIn = null; 
            try {
                uncompressedIn = new GZIPInputStream(gzippedIn);
                if (isFITSFile(uncompressedIn)) {
                    fileType = "application/fits-gzipped";
                }
            } catch (IOException ioex) {
                if (uncompressedIn != null) {
                    try {uncompressedIn.close();} catch (IOException e) {}
                }
            }
        } 
        if ("application/zip".equals(fileType)) {
            
            // Is this a zipped Shapefile?
            // Check for shapefile extensions as described here: http://en.wikipedia.org/wiki/Shapefile
            //logger.info("Checking for shapefile");

            ShapefileHandler shp_handler = new ShapefileHandler(new FileInputStream(f));
             if (shp_handler.containsShapefile()){
              //  logger.info("------- shapefile FOUND ----------");
                 fileType = ShapefileHandler.SHAPEFILE_FILE_TYPE; //"application/zipped-shapefile";
             }
        } 
        
        logger.fine("returning fileType "+fileType);
        return fileType;
    }
    
    public static String determineFileTypeByExtension(String fileName) {
        logger.fine("Type by extension, for "+fileName+": "+MIME_TYPE_MAP.getContentType(fileName));
        return MIME_TYPE_MAP.getContentType(fileName);
    }
    
    
    /* 
     * Custom method for identifying FITS files: 
     * TODO: 
     * the existing check for the "magic header" is very weak (see below); 
     * it should probably be replaced by attempting to parse and read at 
     * least the primary HDU, using the NOM fits parser. 
     * -- L.A. 4.0 alpha
    */
    private static boolean isFITSFile(File file) {
        BufferedInputStream ins = null;

        try {
            ins = new BufferedInputStream(new FileInputStream(file));
            return isFITSFile(ins);
        } catch (IOException ex) {
        } 
        
        return false;
    }
     
    private static boolean isFITSFile(InputStream ins) {
        boolean isFITS = false;

        // number of header bytes read for identification: 
        int magicWordLength = 6;
        String magicWord = "SIMPLE";

        try {
            byte[] b = new byte[magicWordLength];
            logger.fine("attempting to read "+magicWordLength+" bytes from the FITS format candidate stream.");
            if (ins.read(b, 0, magicWordLength) != magicWordLength) {
                throw new IOException();
            }

            if (magicWord.equals(new String(b))) {
                logger.fine("yes, this is FITS file!");
                isFITS = true;
            }
        } catch (IOException ex) {
            isFITS = false; 
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (Exception e) {
                }
            }
        }
    
        return isFITS;
    }
    
    private static boolean isGraphMLFile(File file) {
        boolean isGraphML = false;
        logger.fine("begin isGraphMLFile()");
        try{
            FileReader fileReader = new FileReader(file);
            javax.xml.stream.XMLInputFactory xmlif = javax.xml.stream.XMLInputFactory.newInstance();
            xmlif.setProperty("javax.xml.stream.isCoalescing", java.lang.Boolean.TRUE);

            XMLStreamReader xmlr = xmlif.createXMLStreamReader(fileReader);
            for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (xmlr.getLocalName().equals("graphml")) {
                        String schema = xmlr.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
                        logger.fine("schema = "+schema);
                        if (schema!=null && schema.indexOf("http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd")!=-1){
                            logger.fine("graphML is true");
                            isGraphML = true;
                        }
                    }
                    break;
                }
            }
        } catch(XMLStreamException e) {
            logger.fine("XML error - this is not a valid graphML file.");
            isGraphML = false;
        } catch(IOException e) {
            throw new EJBException(e);
        }
        logger.fine("end isGraphML()");
        return isGraphML;
    }
    
    /**
     * The number of bytes in a kilobyte, megabyte and gigabyte:
     */
    public static final long ONE_KB = 1024;
    public static final long ONE_MB = ONE_KB * ONE_KB;
    public static final long ONE_GB = ONE_KB * ONE_MB;
 
    public static String getFriendlySize(Long filesize) {
        if (filesize == null || filesize.longValue() < 0) {
            return "unknown";
        }

        long bytesize = filesize.longValue();
        String displaySize;

        if (bytesize / ONE_GB > 0) {
            displaySize = String.valueOf(bytesize / ONE_GB) + "." + String.valueOf((bytesize % ONE_GB) / (100 * ONE_MB)) + " GB";
        } else if (bytesize / ONE_MB > 0) {
            displaySize = String.valueOf(bytesize / ONE_MB) + "." + String.valueOf((bytesize % ONE_MB) / (100 * ONE_KB)) + " MB";
        } else if (bytesize / ONE_KB > 0) {
            displaySize = String.valueOf(bytesize / ONE_KB) + "." + String.valueOf((bytesize % ONE_KB) / 100) + " KB";
        } else {
            displaySize = String.valueOf(bytesize) + " bytes";
        }
        return displaySize;

    }

    // from MD5Checksum.java
    public static String CalculateCheckSum(String datafile, ChecksumType checksumType) {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(datafile);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }

        return CalculateChecksum(fis, checksumType);
    }

    // from MD5Checksum.java
    public static String CalculateChecksum(InputStream in, ChecksumType checksumType) {
        MessageDigest md = null;
        try {
            // Use "SHA-1" (toString) rather than "SHA1", for example.
            md = MessageDigest.getInstance(checksumType.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        byte[] dataBytes = new byte[1024];

        int nread;
        try {
            while ((nread = in.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }

        byte[] mdbytes = md.digest();
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static String generateOriginalExtension(String fileType) {

        if (fileType.equalsIgnoreCase("application/x-spss-sav")) {
            return ".sav";
        } else if (fileType.equalsIgnoreCase("application/x-spss-por")) {
            return ".por";
        } else if (fileType.equalsIgnoreCase("application/x-stata")) {
            return ".dta";
        } else if (fileType.equalsIgnoreCase( "application/x-rlang-transport")) {
            return ".RData";
        } else if (fileType.equalsIgnoreCase("text/csv")) {
            return ".csv";
        } else if (fileType.equalsIgnoreCase( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ".xlsx";
        }

        return "";
    }
    
    public static List<DataFile> createDataFiles(DatasetVersion version, InputStream inputStream, String fileName, String suppliedContentType, SystemConfig systemConfig) throws IOException {
        List<DataFile> datafiles = new ArrayList<>(); 
        
        String warningMessage = null; 
        
        // save the file, in the temporary location for now: 
        Path tempFile = null; 
        
        
        if (getFilesTempDirectory() != null) {
            tempFile = Files.createTempFile(Paths.get(getFilesTempDirectory()), "tmp", "upload");
            // "temporary" location is the key here; this is why we are not using 
            // the DataStore framework for this - the assumption is that 
            // temp files will always be stored on the local filesystem. 
            //          -- L.A. Jul. 2014
            logger.fine("Will attempt to save the file as: " + tempFile.toString());
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new IOException ("Temp directory is not configured.");
        }
        logger.fine("mime type supplied: "+suppliedContentType);
        // Let's try our own utilities (Jhove, etc.) to determine the file type 
        // of the uploaded file. (We may already have a mime type supplied for this
        // file - maybe the type that the browser recognized on upload; or, if 
        // it's a harvest, maybe the remote server has already given us the type
        // for this file... with our own type utility we may or may not do better 
        // than the type supplied:
        //  -- L.A. 
        String recognizedType = null;
        String finalType = null; 
        try {
            recognizedType = determineFileType(tempFile.toFile(), fileName);
            logger.fine("File utility recognized the file as " + recognizedType);
            if (recognizedType != null && !recognizedType.equals("")) {
                // is it any better than the type that was supplied to us,
                // if any?
                // This is not as trivial a task as one might expect... 
                // We may need a list of "good" mime types, that should always
                // be chosen over other choices available. Maybe it should 
                // even be a weighed list... as in, "application/foo" should 
                // be chosen over "application/foo-with-bells-and-whistles".
                
                // For now the logic will be as follows: 
                //
                // 1. If the contentType supplied (by the browser, most likely) 
                // is some form of "unknown", we always discard it in favor of 
                // whatever our own utilities have determined; 
                // 2. We should NEVER trust the browser when it comes to the 
                // following "ingestable" types: Stata, SPSS, R;
                // 2a. We are willing to TRUST the browser when it comes to
                //  the CSV and XSLX ingestable types.
                // 3. We should ALWAYS trust our utilities when it comes to 
                // ingestable types. 
                
                if (suppliedContentType == null
                        || suppliedContentType.equals("")
                        || suppliedContentType.equalsIgnoreCase(MIME_TYPE_UNDETERMINED_DEFAULT)
                        || suppliedContentType.equalsIgnoreCase(MIME_TYPE_UNDETERMINED_BINARY)
                        || (ingestableAsTabular(suppliedContentType)
                            && !suppliedContentType.equalsIgnoreCase(MIME_TYPE_CSV)
                            && !suppliedContentType.equalsIgnoreCase(MIME_TYPE_CSV_ALT)
                            && !suppliedContentType.equalsIgnoreCase(MIME_TYPE_XLSX))
                        || ingestableAsTabular(recognizedType)
                        || recognizedType.equals("application/fits-gzipped")
                        || recognizedType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)
                        || recognizedType.equals(MIME_TYPE_ZIP)) {
                    finalType = recognizedType;
                }
            }
            
        } catch (Exception ex) {
            logger.warning("Failed to run the file utility mime type check on file " + fileName);
        }
        
        if (finalType == null) {
            finalType = (suppliedContentType == null || suppliedContentType.equals("")) 
                ? MIME_TYPE_UNDETERMINED_DEFAULT
                : suppliedContentType;
        }
                
        // A few special cases: 
        
        // if this is a gzipped FITS file, we'll uncompress it, and ingest it as
        // a regular FITS file:
        
        if (finalType.equals("application/fits-gzipped")) {

            InputStream uncompressedIn = null;
            String finalFileName = fileName;
            // if the file name had the ".gz" extension, remove it, 
            // since we are going to uncompress it:
            if (fileName != null && fileName.matches(".*\\.gz$")) {
                finalFileName = fileName.replaceAll("\\.gz$", "");
            }
            
            DataFile datafile = null; 
            try {                
                uncompressedIn = new GZIPInputStream(new FileInputStream(tempFile.toFile()));  
                datafile = createSingleDataFile(version, uncompressedIn, finalFileName, MIME_TYPE_UNDETERMINED_DEFAULT, systemConfig.getFileFixityChecksumAlgorithm());
            } catch (IOException ioex) {
                datafile = null;
            } finally {
                if (uncompressedIn != null) {
                    try {uncompressedIn.close();} catch (IOException e) {}
                }
            }
            
            // If we were able to produce an uncompressed file, we'll use it 
            // to create and return a final DataFile; if not, we're not going
            // to do anything - and then a new DataFile will be created further 
            // down, from the original, uncompressed file.
            if (datafile != null) {
                // remove the compressed temp file: 
                try {
                    tempFile.toFile().delete();
                } catch (SecurityException ex) {
                    // (this is very non-fatal)
                    logger.warning("Failed to delete temporary file "+tempFile.toString());
                }
                
                datafiles.add(datafile);
                return datafiles;
            }
                
        // If it's a ZIP file, we are going to unpack it and create multiple 
        // DataFile objects from its contents:
          } else if (finalType.equals("application/zip")) {   
            
            ZipInputStream unZippedIn = null; 
            ZipEntry zipEntry = null; 
            
            int fileNumberLimit = systemConfig.getZipUploadFilesLimit();
            
            try {
                Charset charset = null;
                /*
                TODO: (?)
                We may want to investigate somehow letting the user specify 
                the charset for the filenames in the zip file...
                - otherwise, ZipInputStream bails out if it encounteres a file 
                name that's not valid in the current charest (i.e., UTF-8, in 
                our case). It would be a bit trickier than what we're doing for 
                SPSS tabular ingests - with the lang. encoding pulldown menu - 
                because this encoding needs to be specified *before* we upload and
                attempt to unzip the file. 
                        -- L.A. 4.0 beta12
                logger.info("default charset is "+Charset.defaultCharset().name());
                if (Charset.isSupported("US-ASCII")) {
                    logger.info("charset US-ASCII is supported.");
                    charset = Charset.forName("US-ASCII");
                    if (charset != null) {
                        logger.info("was able to obtain charset for US-ASCII");
                    }
                    
                }
                */
                
                if (charset != null) {
                    unZippedIn = new ZipInputStream(new FileInputStream(tempFile.toFile()), charset);
                } else {
                    unZippedIn = new ZipInputStream(new FileInputStream(tempFile.toFile()));
                } 

                while (true) { 
                    try {
                        zipEntry = unZippedIn.getNextEntry();
                    } catch (IllegalArgumentException iaex) {
                        // Note: 
                        // ZipInputStream documentation doesn't even mention that 
                        // getNextEntry() throws an IllegalArgumentException!
                        // but that's what happens if the file name of the next
                        // entry is not valid in the current CharSet. 
                        //      -- L.A.
                        warningMessage = "Failed to unpack Zip file. (Unknown Character Set used in a file name?) Saving the file as is.";
                        logger.warning(warningMessage);
                        throw new IOException();
                    } 
                    
                    if (zipEntry == null) {
                        break;
                    }
                    // Note that some zip entries may be directories - we 
                    // simply skip them:
                    
                    if (!zipEntry.isDirectory()) {
                        if (datafiles.size() > fileNumberLimit) {
                            logger.warning("Zip upload - too many files.");
                            warningMessage = "The number of files in the zip archive is over the limit (" + fileNumberLimit + 
                                "); please upload a zip archive with fewer files, if you want them to be ingested " +
                                "as individual DataFiles.";
                            throw new IOException();
                        }

                        String fileEntryName = zipEntry.getName();
                        logger.fine("ZipEntry, file: "+fileEntryName);

                        if (fileEntryName != null && !fileEntryName.equals("")) {

                            String shortName = fileEntryName.replaceFirst("^.*[\\/]", "");

                            // Check if it's a "fake" file - a zip archive entry 
                            // created for a MacOS X filesystem element: (these 
                            // start with "._")
                            if (!shortName.startsWith("._") && !shortName.startsWith(".DS_Store") && !"".equals(shortName)) {
                                // OK, this seems like an OK file entry - we'll try 
                                // to read it and create a DataFile with it:

                                DataFile datafile = createSingleDataFile(version, unZippedIn, shortName, MIME_TYPE_UNDETERMINED_DEFAULT, systemConfig.getFileFixityChecksumAlgorithm(), false);

                                if (!fileEntryName.equals(shortName)) {
                                    // If the filename looks like a hierarchical folder name (i.e., contains slashes and backslashes),
                                    // we'll extract the directory name, then a) strip the leading and trailing slashes; 
                                    // and b) replace all the back slashes with regular ones and b) replace any multiple 
                                    // slashes with a single slash:
                                    String directoryName = fileEntryName.replaceFirst("[\\/][\\/]*[^\\/]*$", "").replaceFirst("^[\\/]*", "").replaceAll("[\\/][\\/]*", "/");
                                    if (!"".equals(directoryName)) {
                                        logger.fine("setting the directory label to " + directoryName);
                                        datafile.getFileMetadata().setDirectoryLabel(directoryName);
                                    }
                                }
                                
                                if (datafile != null) {
                                    // We have created this datafile with the mime type "unknown";
                                    // Now that we have it saved in a temporary location, 
                                    // let's try and determine its real type:
                                    
                                    String tempFileName = getFilesTempDirectory() + "/" + datafile.getStorageIdentifier();
                                    
                                    try {
                                        recognizedType = determineFileType(new File(tempFileName), shortName);
                                        logger.fine("File utility recognized unzipped file as " + recognizedType);
                                        if (recognizedType != null && !recognizedType.equals("")) {
                                             datafile.setContentType(recognizedType);
                                        }
                                    } catch (Exception ex) {
                                        logger.warning("Failed to run the file utility mime type check on file " + fileName);
                                    }
                                    
                                    datafiles.add(datafile);
                                }
                            }
                        }
                    } 
                    unZippedIn.closeEntry(); 
                    
                }
                
            } catch (IOException ioex) {
                // just clear the datafiles list and let 
                // ingest default to creating a single DataFile out
                // of the unzipped file. 
                logger.warning("Unzipping failed; rolling back to saving the file as is.");
                if (warningMessage == null) {
                    warningMessage = "Failed to unzip the file. Saving the file as is.";
                }
                
                datafiles.clear();
            } finally {
                if (unZippedIn != null) {
                    try {unZippedIn.close();} catch (Exception zEx) {}
                }
            }
            if (datafiles.size() > 0) {
                // link the data files to the dataset/version: 
                // (except we no longer want to do this! -- 4.6)
                /*Iterator<DataFile> itf = datafiles.iterator();
                while (itf.hasNext()) {
                    DataFile datafile = itf.next();
                    datafile.setOwner(version.getDataset());
                    if (version.getFileMetadatas() == null) {
                        version.setFileMetadatas(new ArrayList());
                    }
                    version.getFileMetadatas().add(datafile.getFileMetadata());
                    datafile.getFileMetadata().setDatasetVersion(version);
                    
                    version.getDataset().getFiles().add(datafile);
                } */
                // remove the uploaded zip file: 
                try {
                    Files.delete(tempFile);
                } catch (IOException ioex) {
                    // do nothing - it's just a temp file.
                    logger.warning("Could not remove temp file "+tempFile.getFileName().toString());
                }
                // and return:
                return datafiles;
            }
            
        } else if (finalType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)) {
            // Shape files may have to be split into multiple files, 
            // one zip archive per each complete set of shape files:
                       
            //File rezipFolder = new File(this.getFilesTempDirectory());
            File rezipFolder = getShapefileUnzipTempDirectory();
            
            IngestServiceShapefileHelper shpIngestHelper;
            shpIngestHelper = new IngestServiceShapefileHelper(tempFile.toFile(), rezipFolder);

            boolean didProcessWork = shpIngestHelper.processFile();
            if (!(didProcessWork)){            
                logger.severe("Processing of zipped shapefile failed.");
                return null;
            }
            for (File finalFile : shpIngestHelper.getFinalRezippedFiles()){
                FileInputStream finalFileInputStream = new FileInputStream(finalFile);
                finalType = determineContentType(finalFile);
                if (finalType==null){
                    logger.warning("Content type is null; but should default to 'MIME_TYPE_UNDETERMINED_DEFAULT'");
                    continue; 
                }               
                DataFile new_datafile = createSingleDataFile(version, finalFileInputStream, finalFile.getName(), finalType, systemConfig.getFileFixityChecksumAlgorithm());
                if (new_datafile != null) {
                  datafiles.add(new_datafile);
                }else{
                  logger.severe("Could not add part of rezipped shapefile. new_datafile was null: " + finalFile.getName());
                }
                finalFileInputStream.close();                
             
            }
            
            // Delete the temp directory used for unzipping
            
            //logger.fine("Delete temp shapefile unzip directory: " + rezipFolder.getAbsolutePath());
            //FileUtils.deleteDirectory(rezipFolder);

            //// Delete rezipped files
            //for (File finalFile : shpIngestHelper.getFinalRezippedFiles()){
            //    if (finalFile.isFile()){
            //        finalFile.delete();
            //    }
            //}
             
            if (datafiles.size() > 0) {
                return datafiles;
            }else{
                logger.severe("No files added from directory of rezipped shapefiles");
            }
            return null;
           
        } 
        // Finally, if none of the special cases above were applicable (or 
        // if we were unable to unpack an uploaded file, etc.), we'll just 
        // create and return a single DataFile:
        // (Note that we are passing null for the InputStream; that's because
        // we already have the file saved; we'll just need to rename it, below)
        
        DataFile datafile = createSingleDataFile(version, null, fileName, finalType, systemConfig.getFileFixityChecksumAlgorithm());
        
        if (datafile != null) {
            generateStorageIdentifier(datafile);
            if (!tempFile.toFile().renameTo(new File(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier()))) {
                return null; 
            }

            /* We need to calculate the checksum here. createSingleDataFile() method
               calculates checksums when called with a non-null inputstream; for example, 
               on unzipped or un-gzipped files. */
            try {
                // We persist "SHA1" rather than "SHA-1".
                datafile.setChecksumType(systemConfig.getFileFixityChecksumAlgorithm());
                datafile.setChecksumValue(CalculateCheckSum(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier(), datafile.getChecksumType()));
            } catch (Exception md5ex) {
                logger.warning("Could not calculate " + systemConfig.getFileFixityChecksumAlgorithm() + " signature for new file " + fileName);
            }
        
            if (warningMessage != null) {
                createIngestFailureReport(datafile, warningMessage);
                datafile.SetIngestProblem();
            }
            datafiles.add(datafile);
            
            return datafiles;
        }
        
        return null;
    }   // end createDataFiles
     
    /* 
     * This method creates a DataFile, and also saves the bytes from the suppplied 
     * InputStream in the temporary location. 
     * This method should only be called by the upper-level methods that handle 
     * file upload and creation for individual use cases - a single file upload, 
     * an upload of a zip archive that needs to be unpacked and turned into 
     * individual files, etc., and once the file name and mime type have already 
     * been figured out. 
    */
    
    private static DataFile createSingleDataFile(DatasetVersion version, InputStream inputStream, String fileName, String contentType, DataFile.ChecksumType checksumType) {
        return createSingleDataFile(version, inputStream, fileName, contentType, checksumType, false);
    }
    
    private static DataFile createSingleDataFile(DatasetVersion version, InputStream inputStream, String fileName, String contentType, DataFile.ChecksumType checksumType, boolean addToDataset) {

        DataFile datafile = new DataFile(contentType);
        datafile.setModificationTime(new Timestamp(new Date().getTime()));
        /**
         * @todo Think more about when permissions on files are modified.
         * Obviously, here at create time files have some sort of permissions,
         * even if these permissions are *implied*, by ViewUnpublishedDataset at
         * the dataset level, for example.
         */
        datafile.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        FileMetadata fmd = new FileMetadata();
        
        // TODO: add directoryLabel?
        // *this check must be done later, after we drop any duplicates by content* //
        //fmd.setLabel(checkForDuplicateFileNames(version,fileName));
        fmd.setLabel(fileName);

        if (addToDataset) {
            datafile.setOwner(version.getDataset());
        }
        fmd.setDataFile(datafile);
        datafile.getFileMetadatas().add(fmd);
        if (addToDataset) {
            if (version.getFileMetadatas() == null) {
                version.setFileMetadatas(new ArrayList());
            }
            version.getFileMetadatas().add(fmd);
            fmd.setDatasetVersion(version);
            version.getDataset().getFiles().add(datafile);
        }

        // And save the file - but only if the InputStream is not null; 
        // (the temp file may be saved already - if this is a single
        // file upload case - and in that case this method gets called 
        // with null for the inputStream)
        
        if (inputStream != null) {
        
            generateStorageIdentifier(datafile);
            BufferedOutputStream outputStream = null;

            // Once again, at this point we are dealing with *temp*
            // files only; these are always stored on the local filesystem, 
            // so we are using FileInput/Output Streams to read and write
            // these directly, instead of going through the Data Access 
            // framework. 
            //      -- L.A.
            
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier()));

                byte[] dataBuffer = new byte[8192];
                int i = 0;

                while ((i = inputStream.read(dataBuffer)) > 0) {
                    outputStream.write(dataBuffer, 0, i);
                    outputStream.flush();
                }
            } catch (IOException ioex) {
                datafile = null; 
            } finally {
                try {
                    outputStream.close();
                } catch (IOException ioex) {}
            }

            /**
             * @todo Can this block and the similar block above be refactored
             * into a common code path? - yeah, sure. 
             */
            if (datafile != null) {
                try {
                    // We persist "SHA1" rather than "SHA-1".
                    datafile.setChecksumType(checksumType);
                    datafile.setChecksumValue(CalculateCheckSum(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier(), datafile.getChecksumType()));
                } catch (Exception cksumEx) {
                    logger.warning("Could not calculate " + checksumType + " signature for the new file " + fileName);
                }
            }
        }
        
        return datafile;
    }
    
    
    /**
        For the restructuring of zipped shapefiles, create a timestamped directory.
        This directory is deleted after successful restructuring.
    
        Naming convention: getFilesTempDirectory() + "shp_" + "yyyy-MM-dd-hh-mm-ss-SSS"
    */
    private static File getShapefileUnzipTempDirectory(){
        
        String tempDirectory = getFilesTempDirectory();
        if (tempDirectory == null){
            logger.severe("Failed to retrieve tempDirectory, null was returned" );
            return null;
        }
        String datestampedFileName =  "shp_" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS").format(new Date());
        String datestampedFolderName = tempDirectory + "/" + datestampedFileName;
        
        File datestampedFolder = new File(datestampedFolderName);
        if (!datestampedFolder.isDirectory()) {
            /* Note that "createDirectories()" must be used - not 
             * "createDirectory()", to make sure all the parent 
             * directories that may not yet exist are created as well. 
             */
            try {
                Files.createDirectories(Paths.get(datestampedFolderName));
            } catch (IOException ex) {
                logger.severe("Failed to create temp. directory to unzip shapefile: " + datestampedFolderName );
                return null;
            }
        }
        return datestampedFolder;        
    }
    
    public static boolean ingestableAsTabular(DataFile dataFile) {
        String mimeType = dataFile.getContentType();
        
        return ingestableAsTabular(mimeType);
    } 
    
    public static boolean ingestableAsTabular(String mimeType) {
        /* 
         * In the final 4.0 we'll be doing real-time checks, going through the 
         * available plugins and verifying the lists of mime types that they 
         * can handle. In 4.0 beta, the ingest plugins are still built into the 
         * main code base, so we can just go through a hard-coded list of mime 
         * types. -- L.A. 
         */
        
        if (mimeType == null) {
            return false;
        }
        
        if (mimeType.equals(MIME_TYPE_STATA)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_STATA13)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_RDATA)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_CSV) || mimeType.equals(MIME_TYPE_CSV_ALT)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_XLSX)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_SPSS_SAV)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_SPSS_POR)) {
            return true;
        }

        return false;
    }
    
    public static String getFilesTempDirectory() {
        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }

        String filesTempDirectory = filesRootDirectory + "/temp";

        if (!Files.exists(Paths.get(filesTempDirectory))) {
            /* Note that "createDirectories()" must be used - not 
             * "createDirectory()", to make sure all the parent 
             * directories that may not yet exist are created as well. 
             */
            try {
                Files.createDirectories(Paths.get(filesTempDirectory));
            } catch (IOException ex) {
                logger.severe("Failed to create filesTempDirectory: " + filesTempDirectory );
                return null;
            }
        }

        return filesTempDirectory;
    }
    
    public static void generateStorageIdentifier(DataFile dataFile) {
        dataFile.setStorageIdentifier(generateStorageIdentifier());
    }
    
    public static String generateStorageIdentifier() {
        
        UUID uid = UUID.randomUUID();
                
        logger.log(Level.FINE, "UUID value: {0}", uid.toString());
        
        // last 6 bytes, of the random UUID, in hex: 
        
        String hexRandom = uid.toString().substring(24);
        
        logger.log(Level.FINE, "UUID (last 6 bytes, 12 hex digits): {0}", hexRandom);
        
        String hexTimestamp = Long.toHexString(new Date().getTime());
        
        logger.log(Level.FINE, "(not UUID) timestamp in hex: {0}", hexTimestamp);
            
        String storageIdentifier = hexTimestamp + "-" + hexRandom;
        
        logger.log(Level.FINE, "timestamp/UUID hybrid: {0}", storageIdentifier);
        return storageIdentifier; 
    }
    
    public static void createIngestFailureReport(DataFile dataFile, String message) {
        createIngestReport(dataFile, IngestReport.INGEST_STATUS_FAILURE, message);
    }
    
    private static void createIngestReport (DataFile dataFile, int status, String message) {
        IngestReport errorReport = new IngestReport();
        if (status == IngestReport.INGEST_STATUS_FAILURE) {
                errorReport.setFailure();
                errorReport.setReport(message);
                errorReport.setDataFile(dataFile);
                dataFile.setIngestReport(errorReport);
        }
    }
    
    
    
}
