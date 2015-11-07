package repositorySearch;

import repositorySearch.exception.DownloadException;
import repositorySearch.exception.UnzipException;
import util.SearchProperties;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class FileHandler {

    private static void registryDownloadProblem(String text) {
        try(FileWriter writer = new FileWriter(SearchProperties.DOWNLOAD_PROBLEMS_FILE, true)){
            writer.append(text).append("\n");
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Downloads a zip file.
     *
     * @param zipUrl url for the download.
     * @param zipPath path to save downloaded zip file.
     * @throws DownloadException if there's an error during downloading.
     */
    public static void downloadZipFile(String zipUrl, String zipPath) throws DownloadException {
        zipUrl = zipUrl.replaceAll(" ","");
        System.out.printf("Downloading zipfile: %s\n", zipUrl);
        BufferedInputStream in;
        try {
            in = new BufferedInputStream(new java.net.URL(zipUrl).openStream());
            FileOutputStream fos = new FileOutputStream(zipPath);
            BufferedOutputStream bout = new BufferedOutputStream(fos, 8192);
            byte[] data = new byte[8192];
            int x;
            while ((x = in.read(data, 0, 8192)) >= 0) {
                bout.write(data, 0, x);
            }
            bout.close();
            in.close();
            System.out.println("Done downloading!");
        } catch (IOException e) {
            registryDownloadProblem(zipUrl);
            throw new DownloadException(e.getMessage());
        }
    }

    /**
     * Unzips repository's zip file and saves it at "unzipped" folder.
     * @param zipPath path of zip file.
     * @param outputFolder place to save zip file content.
     * @throws UnzipException if there's an error during unzipping.
     */
    public static void unzip(String zipPath, String outputFolder) throws UnzipException {
        byte[] buffer = new byte[1024];
        File folder = new File(outputFolder); //create output directory is not exists

        System.out.printf("Unzipping zipfile: %s\n", zipPath);
        try{
            if (!folder.exists()) {
                folder.mkdir();
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath)); //get the zip file content
                ZipEntry ze = zis.getNextEntry(); //get the zipped file list entry

                while (ze != null) {
                    String fileName = ze.getName();
                    File newFile = new File(outputFolder + File.separator + fileName);
                    if (ze.isDirectory()) {
                        new File(newFile.getParent()).mkdirs();
                    } else {
                        FileOutputStream fos;
                        new File(newFile.getParent()).mkdirs();
                        fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    ze = zis.getNextEntry();
                }
                zis.closeEntry();
                zis.close();
                System.out.println("Done unzipping!");
            }
        } catch(IOException e){
            throw new UnzipException(e.getMessage());
        }
    }

    /**
     * Verifies if a folder contains file for a specific type.
     * @param type file type to repositorySearch.
     * @param folder place to repositorySearch.
     */
    public static boolean hasFileType(String type, String folder){
        boolean result = false;
        File dir = new File(folder);
        File[] files = dir.listFiles();

        if(files == null) return false;

        for(File f: files){
            if(result) return true;
            else if(f.isDirectory()) result = hasFileType(type, f.getAbsolutePath());
            else {
                if(f.getName().endsWith(type)) {
                    result = true;
                    //System.out.printf("Founded feature file: %s%n", f.getAbsolutePath());
                }
            }
        }
        return result;
    }

    /**
     * Deletes a folder and its files.
     *
     * @param folder folder's path.
     */
    public static void deleteFolder(String folder) {
        File dir = new File(folder);
        File[] files = dir.listFiles();
        if(files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f.getAbsolutePath());
                } else f.delete();
            }
        }
        dir.delete();
    }

    /**
     * Deletes a zip file if it does exist.
     *
     * @param path zip file path.
     */
    public static void deleteZipFile(String path) {
        File file = new File(path);
        file.delete();
    }

}
