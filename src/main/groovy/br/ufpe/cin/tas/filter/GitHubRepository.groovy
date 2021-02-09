package br.ufpe.cin.tas.filter

import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.DataProperties

/**
 * Represents a GitHub repository and provides mechanism to download de repository zip file, unzip it and check it has
 * a specific file type.
 */
@Slf4j
class GitHubRepository {

    String url
    String branch
    String createdAt
    int stars
    int size
    String description
    final String name
    String zipUrl


    GitHubRepository(String url, String branch, String createdAt, int stars, int size, String description) {
        this.url = url
        this.branch = branch
        this.createdAt = createdAt
        this.stars = stars
        this.size = size
        this.description = description
        this.name = url?.substring(ConstantData.GITHUB_URL.length())?.replaceAll("/", "_")
        configureZipUrl()
    }

    GitHubRepository(String zipFileUrl) {
        this.url = zipFileUrl.substring(0, zipFileUrl.indexOf(ConstantData.ZIP_FILE_URL))
        this.branch = zipFileUrl.substring(zipFileUrl.lastIndexOf("/") + 1, zipFileUrl.length() - ConstantData.FILE_EXTENSION.length())
        this.name = url?.substring(ConstantData.GITHUB_URL.length())?.replaceAll("/", "_")
        configureZipUrl()
    }

    private void configureZipUrl() {
        this.zipUrl = url + ConstantData.ZIP_FILE_URL + branch + ConstantData.FILE_EXTENSION
    }

    String getZipFolderName() {
        ConstantData.UNZIPPED_FILES_FOLDER + name
    }

    String getLocalZipName() {
        ConstantData.ZIPPED_FILES_FOLDER + name + ConstantData.FILE_EXTENSION
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
     * @throws Exception if there's an error during downloading.
     */
    def downloadZip() throws Exception {
        FileHandler.downloadZipFile(zipUrl, getLocalZipName())
    }

    /**
     * Unzips repository's zip file and saves it at "unzipped" folder.
     *
     * @throws Exception if there's an error during unzipping.
     */
    def unzip() throws Exception {
        FileHandler.unzip(getLocalZipName(), getZipFolderName())
    }

    boolean satisfiesFilteringCriteria() {
        boolean result = false
        if (DataProperties.FILTER_BY_FILE){
            try {
                downloadZip()
                unzip()
                result = FileHandler.hasFileType(getZipFolderName())
                deleteUnzippedDir()
            } catch (Exception e) {
                log.info e.getMessage()
            }
        }
        else result = true
        result
    }

    /**
     * Deletes unzipped content (folder and files) of the repository, if it does exist.
     */
    def deleteUnzippedDir() {
        FileHandler.deleteFolder(getZipFolderName())
    }

    /**
     * Deletes the zip file of the repository, if it does exist.
     */
    def deleteZipFile() {
        FileHandler.deleteZipFile(getLocalZipName())
    }

    /**
     * Deletes the zip file and the unzipped content (folder and files) of the repository, if it does exist.
     */
    def deleteAll() {
        FileHandler.deleteFolder(getZipFolderName())
        FileHandler.deleteZipFile(getLocalZipName())
    }

    @Override
    String toString(){
        return "Repository: $name ($url); created at: $createdAt"
    }
}
