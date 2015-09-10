package search;

import java.io.IOException;


/*everything related to git repo 
 * step 1: read input file --ok
 * step 2: download zipped commits from github --ok
 * step 3: unzip commits --ok
 * step 4: load commits directories --ok
 * obs: limit the loadDirectory method to file extensions and folder that matter --ok
 * obs: rename commit root directory from rgms-shaKey to "sourcecode" --ok
 * 
 * step 5: compute features changeset --ok (could be optimized)
 * changes suggested by Paulo Borba:
 * remove changeset as an atribute of class feature --ok
 * step 6: compute features intersection --ok
 * step 7: write changesets and intersections reports --ok
 * improvements:
 * 1-rename FeatureCommit to TaskCommit --ok
 * 2-add reference two both commits on a changeset instance --ok
 * */

public class Repository {

    private String url;
    private String branch;
    private String name;
    private FileHandler fileHandler;

    public static String GITHUB_URL = "https://github.com/";

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
        String name = url.substring(GITHUB_URL.length());
        name = name.replaceAll("/", "_");
        return name;
    }

    public String getZipfileName(){
        return name+ Util.FILE_EXTENSION;
    }

    public void downloadCommits() {
        try {
            fileHandler.downloadZipFile(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unzipCommits() {
        try {
            fileHandler.unzipper(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
