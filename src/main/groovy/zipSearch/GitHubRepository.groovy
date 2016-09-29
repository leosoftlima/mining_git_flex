package zipSearch

import util.DataProperties
import zipSearch.exception.DownloadException
import zipSearch.exception.UnzipException


/**
 * Represents a GitHub repository and provides mechanism to download de repository zip file, unzip it and check it has
 * Gherkin files (extension .feature).
 */
public class GitHubRepository {

    String url
    String branch
    final String name
    String zipUrl

    GitHubRepository(String url, String branch) {
        this.url = url
        this.branch = branch
        this.name = configureName(url)
        configureZipUrl()
    }

    GitHubRepository(String zipFileUrl) {
        this.url = zipFileUrl.substring(0,zipFileUrl.indexOf(DataProperties.ZIP_FILE_URL))
        this.branch = zipFileUrl.substring(zipFileUrl.lastIndexOf("/") + 1, zipFileUrl.length() - DataProperties.FILE_EXTENSION.length())
        this.name = configureName(url)
        configureZipUrl()
    }

    private static String configureName(String url){
        println url
        String name = url.substring(DataProperties.GITHUB_URL.length())
        name = name.replaceAll("/", "_")
        return name
    }

    private void configureZipUrl(){
        this.zipUrl = url + DataProperties.ZIP_FILE_URL + branch + DataProperties.FILE_EXTENSION
    }

    public String getZipFolderName() {
        return DataProperties.UNZIPPED_FILES_DIR +name
    }

    public String getLocalZipName(){
        return DataProperties.ZIPPED_FILES_DIR + name+ DataProperties.FILE_EXTENSION
    }

    void setUrl(String url) {
        this.url = url
        configureZipUrl()
    }

    void setBranch(String branch) {
        this.branch = branch
        configureZipUrl()
    }

    /**
     * Downloads repository's main branch as a zip file and saves it at "zipped" folder.
     *
     * @throws zipSearch.exception.DownloadException if there's an error during downloading.
     */
    void downloadZip() throws DownloadException {
        FileHandler.downloadZipFile(zipUrl, getLocalZipName())
    }

    /**
     * Unzips repository's zip file and saves it at "unzipped" folder.
     *
     * @throws zipSearch.exception.UnzipException if there's an error during unzipping.
     */
    void unzip() throws UnzipException {
        FileHandler.unzip(getLocalZipName(), getZipFolderName())
    }

    /**
     * Verifies if the repository main branch contains file of specific type that is specified at the configuration
     * properties file. It's necessary to download the repository as zip file first and unzip it.
     *
     * @param fileType the type of file to search for.
     * @return true if the repository does contain Gherkin file, false otherwise. If no file type is defined, the search
     * is also true.
     */
    boolean hasFileType(String fileType){
        boolean result = false
        try {
            downloadZip()
            unzip()
            result = FileHandler.hasFileType(fileType, getZipFolderName())
        } catch (DownloadException e) {
            System.out.println(e.getMessage())
        } catch (UnzipException e) {
            System.out.println(e.getMessage())
            deleteAll()
        }
        return result
    }

    /**
     * Deletes unzipped content (folder and files) of the repository, if it does exist.
     */
    void deleteUnzipedDir(){
        FileHandler.deleteFolder(getZipFolderName())
    }

    /**
     * Deletes the zip file of the repository, if it does exist.
     */
    void deleteZipFile(){
        FileHandler.deleteZipFile(getLocalZipName())
    }

    /**
     * Deletes the zip file and the unzipped content (folder and files) of the repository, if it does exist.
     */
    void deleteAll(){
        FileHandler.deleteFolder(getZipFolderName())
        FileHandler.deleteZipFile(getLocalZipName())
    }
}
