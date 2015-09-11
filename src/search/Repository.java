package search;

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

    public void downloadCommits() throws IOException {
        fileHandler.downloadZipFile(this);
    }

    public void unzipCommits() throws IOException {
        fileHandler.unzipper(this);
    }

    public boolean hasFeatureFile(){
        String folder = Util.UNZIPPED_FILES_DIR +getName();
        return fileHandler.hasFileType(Util.FEATURE_FILE_EXTENSION, folder);
    }

    public void delete(){
        fileHandler.deleteFolder(Util.UNZIPPED_FILES_DIR + name);
        fileHandler.deleteZipFile(this);
    }

}
