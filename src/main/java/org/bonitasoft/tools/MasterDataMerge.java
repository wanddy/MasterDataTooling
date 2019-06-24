package org.bonitasoft.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;


/**
 * @author Pablo Alonso de Linaje García
 */
public class MasterDataMerge {
    private static final Logger LOGGER = Logger.getLogger(MasterDataMerge.class.getName());

    private static final String PROC_FILE = "diagrams\\Framework Master Data Management-1.0.proc";
    private static final String PAGE_FILE = "web_page\\CrudMasterDatav1\\CrudMasterDatav2.json";
    private static final String BDM_FILE = "bdm\\bom.xml";
    private static final String PROJECT_BDM_FILE = "\\bom.xml";
    private static final String BOM_LAST_LINE = "</businessObjects>";
    private static final String ORIGINAL_PACKAGE_NAME = "com.company.model.MasterData";
    private static final String REGEX = "qualifiedName=\\\"([^\\\"]*)\\\"";
    private static final String MASTER_DATA_BO_TXT = "MasterDataBO.txt";

    public static void main(String[] args) throws IOException {
        // Check how many arguments were passed in

        if(args.length != 1)
        {
            System.out.println("WARNING: The path to your bdm is required");
            System.exit(0);
        }
        String bonitaProject = args[0];

        merge(bonitaProject);

    }

    private static void merge(String bonitaProject) throws IOException {
        String output;
        try {

            // Prepare files
            LOGGER.info("Copy base" );
            String repoPath = copyFiles() + "\\resources\\";
            LOGGER.info("Temp folder " + repoPath );
            //Modify BDM
            LOGGER.info("Modify BDM " );
            String packageName = addBdmObject(repoPath, bonitaProject);
            //Modify proc
            LOGGER.info("Modify Process" );
            modify(repoPath + PROC_FILE, packageName);
            //Modify page
            LOGGER.info("Modify Page" );
            modify(repoPath + PAGE_FILE, packageName);
            LOGGER.info("Zip again" );
            //Zip Again
            compressZipfile(repoPath, "MOD_MasterDataBase.bos");
        }catch(Exception e){
            LOGGER.severe(e.getMessage());
        }

    }

    private static String copyFiles() throws IOException {
        File myTempDir = Files.createTempDirectory("MasterData").toFile();
        FileUtils.copyDirectoryToDirectory(new File("resources"), myTempDir);
        return myTempDir.getAbsolutePath();
    }

    private static String addBdmObject(String folderPath, String bonitaProject) throws IOException {
        Path path = Paths.get(bonitaProject+ PROJECT_BDM_FILE);
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);


        String packageName = findPackageName(content) + ".MasterData";

        String bo = getBO(folderPath);

        bo = bo.replaceAll(ORIGINAL_PACKAGE_NAME, packageName);
        content = content.replaceAll(BOM_LAST_LINE, bo);


        Path internalPath = Paths.get(folderPath+ BDM_FILE);
        Files.write(internalPath, content.getBytes(charset));
        return packageName;
    }

    protected static String findPackageName(String content) {

        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(content);

        if (m.find()) {
            String fullClassName = m.group(1);
            return fullClassName.substring(0, fullClassName.lastIndexOf("."));
        }
        throw new RuntimeException("BDM is not valid");


    }

    private static void modify(String filePath, String packageName) throws IOException {
        Path path = Paths.get(filePath);
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(ORIGINAL_PACKAGE_NAME, packageName);
        Files.write(path, content.getBytes(charset));
    }

    private static void compressZipfile(String sourceDir, String outputFile) throws IOException, FileNotFoundException {
        ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(outputFile));
        Path srcPath = Paths.get(sourceDir);
        compressDirectoryToZipfile(srcPath.getParent().toString(), srcPath.getFileName().toString(), zipFile);
        IOUtils.closeQuietly(zipFile);
    }
    private static void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipOutputStream out) throws IOException, FileNotFoundException {
        String dir = Paths.get(rootDir, sourceDir).toString();
        for (File file : new File(dir).listFiles()) {
            if (file.isDirectory()) {
                compressDirectoryToZipfile(rootDir, Paths.get(sourceDir,file.getName()).toString(), out);
            } else {
                ZipEntry entry = new ZipEntry(Paths.get(sourceDir,file.getName()).toString());
                out.putNextEntry(entry);

                FileInputStream in = new FileInputStream(Paths.get(rootDir, sourceDir, file.getName()).toString());
                IOUtils.copy(in, out);
                IOUtils.closeQuietly(in);
            }
        }
    }
    private static void unzip(Path path, Charset charset) throws IOException{

        String fileBaseName = FilenameUtils.getBaseName(path.getFileName().toString());
        Path destFolderPath = Paths.get(path.getParent().toString(), fileBaseName);

        try (ZipFile zipFile = new ZipFile(path.toFile(), ZipFile.OPEN_READ, charset)){
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destFolderPath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)){
                        try (OutputStream out = new FileOutputStream(entryPath.toFile())){
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }
        }
    }


    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
    private static String getBO(String folderPath) throws IOException {
        LOGGER.info(folderPath+"MasterDataBO.txt");
        Path path = Paths.get(folderPath + MASTER_DATA_BO_TXT);
        Charset charset = StandardCharsets.UTF_8;

        return new String(Files.readAllBytes(path), charset);
    }
}
