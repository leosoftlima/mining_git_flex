package search;

import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class FileHandler {

    private static void registryDownloadProblem(String text) {
        try(FileWriter writer = new FileWriter(Util.PROBLEMS_FILE, true)){
            writer.append(text).append("\n");
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public static ArrayList<Repository> extractProjects() throws IOException {
        ArrayList<Repository> repos = new ArrayList<>();
        CSVReader reader = new CSVReader(new FileReader(Util.PROJECTS_FILE));
        List<String[]> entries = reader.readAll();

        if(entries.size()>0) entries.remove(0); //ignore sheet header

        for(String[] line: entries){
            repos.add(new Repository(line[0], line[1])); //url, branch
        }
        return repos;
    }

    public void downloadZipFile(Repository repo) throws IOException {
        String gitUrl = repo.getUrl() + Util.ZIP_FILE_URL + repo.getBranch()+Util.FILE_EXTENSION;
        System.out.printf("Downloading zipfile: %s\n", gitUrl);
        BufferedInputStream in;
        try {
            in = new BufferedInputStream(new java.net.URL(gitUrl).openStream());
            FileOutputStream fos = new FileOutputStream(Util.ZIPPED_FILES_DIR+"/"+repo.getName()+ Util.FILE_EXTENSION);
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
            System.out.println("Problem during download: "+e.getMessage());
            registryDownloadProblem(gitUrl);
            throw e;
        }
    }

    public void unzipper(Repository repo) throws IOException {
        String zipFile = Util.ZIPPED_FILES_DIR+repo.getZipfileName();
        String outputFolder = Util.UNZIPPED_FILES_DIR +repo.getName();
        byte[] buffer = new byte[1024];
        File folder = new File(outputFolder); //create output directory is not exists

        System.out.printf("Unzipping zipfile: %s\n", zipFile);
        if (!folder.exists()) {
            folder.mkdir();
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile)); //get the zip file content
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
    }

    public boolean hasFileType(String type, String folder){
        boolean result = false;
        File dir = new File(folder);
        File[] files = dir.listFiles();

        if(files == null) return false;

        for(File f: files){
            if(result) return true;
            else if(f.isDirectory()) result = hasFileType(type, f.getAbsolutePath());
            else {
                if(f.getName().contains(type)) result = true;
            }
        }
        return result;
    }

    public void deleteFolder(String folder) {
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

    public void deleteZipFile(String name) {
        File file = new File(Util.ZIPPED_FILES_DIR+"/"+name+ Util.FILE_EXTENSION);
        file.delete();
    }

}
