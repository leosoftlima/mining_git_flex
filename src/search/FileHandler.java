package search;

import au.com.bytecode.opencsv.CSVReader;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class FileHandler {

    public static String PROJECTS_FILE = "input/projects.csv";
    public static String ZIP_FILE_URL = "/archive/";
    public static String ZIPPED_FILES_DIR = "zipped/";
    public static String UNZIPPED_FILES_DIR = "unzipped/";

    public static ArrayList<Repository> extractProjects() throws IOException {
        ArrayList<Repository> repos = new ArrayList<>();
        CSVReader reader = new CSVReader(new FileReader(PROJECTS_FILE));
        List<String[]> entries = reader.readAll();

        if(entries.size()>0) entries.remove(0); //ignore sheet header

        for(String[] line: entries){
            repos.add(new Repository(line[0], line[3])); //url, branch
        }
        return repos;
    }

    public void downloadZipFile(Repository repo) throws IOException {
        String gitUrl = repo.getUrl() + ZIP_FILE_URL + repo.getBranch()+Util.FILE_EXTENSION;
        System.out.printf("Downloading zipfile: %s\n", gitUrl);
        BufferedInputStream in;
        try {
            in = new BufferedInputStream(new java.net.URL(gitUrl).openStream());
            FileOutputStream fos = new FileOutputStream(ZIPPED_FILES_DIR+"/"+repo.getName()+ Util.FILE_EXTENSION);
            BufferedOutputStream bout = new BufferedOutputStream(fos, 8192);
            byte[] data = new byte[8192];
            int x;
            while ((x = in.read(data, 0, 8192)) >= 0) {
                bout.write(data, 0, x);
            }
            bout.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done downloading!");
    }

    public void unzipper(Repository repo) throws IOException {
        String outputFolder = UNZIPPED_FILES_DIR +repo.getName();
        byte[] buffer = new byte[1024];
        File folder = new File(outputFolder); //create output directory is not exists

        System.out.printf("Unzipping zipfile: %s\n", repo.getZipfileName());
        if (!folder.exists()) {
            folder.mkdir();
            ZipInputStream zis; //get the zip file content

            try {
                zis = new ZipInputStream(new FileInputStream(repo.getZipfileName()));
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Done unzipping!");
    }

}
