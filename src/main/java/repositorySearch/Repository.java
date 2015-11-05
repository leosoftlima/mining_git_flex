package repositorySearch;

import repositorySearch.exception.DownloadException;
import repositorySearch.exception.UnzipException;
import util.Util;

import java.io.File;


/**
 * Represents a GitHub repository and provides mechanism to download de repository zip file, unzip it and check it has
 * Gherkin files (extension .feature).
 */
public class Repository {

    private String url;
    private String branch;
    private final String name;
    private String zipUrl;

    public Repository(String url, String branch) {
        this.url = url;
        this.branch = branch;
        this.name = configureName(url);
        configureZipUrl();
    }

    public Repository(String zipFileUrl) {
        this.url = zipFileUrl.substring(0,zipFileUrl.indexOf(Util.ZIP_FILE_URL));
        this.branch = zipFileUrl.substring(zipFileUrl.lastIndexOf("/") + 1, zipFileUrl.length() - Util.FILE_EXTENSION.length());
        this.name = configureName(url);
        configureZipUrl();
    }

    private String configureName(String url){
        String name = url.substring(Util.GITHUB_URL.length());
        name = name.replaceAll("/", "_");
        return name;
    }

    private void configureZipUrl(){
        this.zipUrl = url + Util.ZIP_FILE_URL + branch + Util.FILE_EXTENSION;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        configureZipUrl();
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
        configureZipUrl();
    }

    public String getName() {
        return name;
    }

    public String getLocalZipName(){
        return Util.ZIPPED_FILES_DIR + name+ Util.FILE_EXTENSION;
    }

    public String getZipUrl(){
        return zipUrl;
    }

    /**
     * Downloads repository's main branch as a zip file and saves it at "zipped" folder.
     *
     * @throws DownloadException if there's an error during downloading.
     */
    public void downloadZip() throws DownloadException {
        FileHandler.downloadZipFile(zipUrl, getLocalZipName());
    }

    /**
     * Unzips repository's zip file and saves it at "unzipped" folder.
     *
     * @throws UnzipException if there's an error during unzipping.
     */
    public void unzip() throws UnzipException {
        String outputFolder = Util.UNZIPPED_FILES_DIR +name;
        String zipname = getLocalZipName();
        FileHandler.unzip(zipname, outputFolder);
    }

    /**
     * Verifies if the repository main branch contains Gherkin file. It's necessary to download the repository
     * as zip file first and unzip it.
     *
     * @return true if the repository does contain Gherkin file, false otherwise.
     */
    public boolean hasGherkinFile(){
        String folder = Util.UNZIPPED_FILES_DIR +getName();
        boolean result = false;
        try {
            downloadZip();
            unzip();
            result = FileHandler.hasFileType(Util.FEATURE_FILE_EXTENSION, folder);
        } catch (DownloadException e) {
            System.out.println(e.getMessage());
        } catch (UnzipException e) {
            System.out.println(e.getMessage());
            deleteAll();
        }
        return result;
    }

    /**
     * Deletes unzipped content (folder and files) of the repository, if it does exist.
     */
    public void deleteUnzipedDir(){
        FileHandler.deleteFolder(Util.UNZIPPED_FILES_DIR + name);
    }

    /**
     * Deletes the zip file of the repository, if it does exist.
     */
    public void deleteZipFile(){
        FileHandler.deleteZipFile(getLocalZipName());
    }

    /**
     * Deletes the zip file and the unzipped content (folder and files) of the repository, if it does exist.
     */
    public void deleteAll(){
        String path = Util.UNZIPPED_FILES_DIR + name;
        FileHandler.deleteFolder(path);
        FileHandler.deleteZipFile(getLocalZipName());
    }
}
