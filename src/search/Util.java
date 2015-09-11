package search;

import java.util.List;


public class Util {

    public static String FILE_EXTENSION = ".zip";
    public static String FEATURE_FILE_EXTENSION = ".feature";
    public static String PROJECTS_FILE = "input/projects.csv";
    public static String ZIP_FILE_URL = "/archive/";
    public static String ZIPPED_FILES_DIR = "zipped/";
    public static String UNZIPPED_FILES_DIR = "unzipped/";
    public static String PROBLEMS_FILE = "unzipped/problems.txt";
    public static String GITHUB_URL = "https://github.com/";

    private boolean hasFeatureFile(List<String> files){
        boolean result = false;
        System.out.println("em hasFeatureFile: files size = "+files.size());

        for (String file : files) {
            System.out.println("file: " + file);
            if (file.contains(FEATURE_FILE_EXTENSION)) {
                System.out.println("feature file: " + file);
                result = true;
                break;
            }
        }
        return result;
    }

}