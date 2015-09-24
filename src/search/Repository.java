package search;

import util.Util;
import java.io.IOException;


public class Repository {

    private String url;
    private String branch;
    private String name;
    private FileHandler fileHandler;

    public Repository(String url, String branch) {
        this.url = url;
        this.branch = branch;
        this.name = configureName(url);
        this.fileHandler = new FileHandler();
    }

    public Repository(String gitUrl) {
        this.url = gitUrl.substring(0,gitUrl.indexOf("/archive/"));
        this.branch = gitUrl.substring(gitUrl.lastIndexOf("/")+1, gitUrl.length()- Util.FILE_EXTENSION.length());
        this.name = configureName(url);
        this.fileHandler = new FileHandler();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getName() {
        return name;
    }

    private String configureName(String url){
        String name = url.substring(Util.GITHUB_URL.length());
        name = name.replaceAll("/", "_");
        return name;
    }

    public String getZipfileName(){
        return name+ Util.FILE_EXTENSION;
    }

    public void downloadZip() throws IOException {
        fileHandler.downloadZipFile(this);
    }

    public void unzip() {
        fileHandler.unzipper(this);
    }

    public boolean hasFeatureFile(){
        String folder = Util.UNZIPPED_FILES_DIR +getName();
        return fileHandler.hasFileType(Util.FEATURE_FILE_EXTENSION, folder);
    }

    public void deleteAll(){
        fileHandler.deleteFolder(Util.UNZIPPED_FILES_DIR + name);
        fileHandler.deleteZipFile(name);
    }

    public void deleteUnzipedDir(){
        fileHandler.deleteFolder(Util.UNZIPPED_FILES_DIR + name);
    }

    public void deleteZipFile(){
        fileHandler.deleteZipFile(name);
    }
}
