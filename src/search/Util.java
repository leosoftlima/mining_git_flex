package search;


import java.util.List;

public class Util {

    public static String FILE_EXTENSION = ".zip";
    public static String FEATURE_FILE_EXTENSION = ".feature";

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
