package input;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import util.SearchProperties;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class InputManager {

    /**
     * Generates a new csv file (input/projects-input.csv) based on the one that contains the result of BigQuery engine,
     * excluding duplicated lines and unnecessary columns.
     *
     * @param csvPath csv file that contains the result of BigQuery engine (repository url and master branch)
     * @throws IOException if there's an error reading or writting csv files
     */
    public static void prepareInput(String csvPath) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(csvPath));
        List<String[]> entries = reader.readAll();
        reader.close();

        List<String[]> unique = removeDuplicatedValues(entries);
        CSVWriter writer = new CSVWriter(new FileWriter(SearchProperties.REPOSITORIES_TO_DOWNLOAD_FILE));
        for (String[] line : unique) {
            writer.writeNext(new String[]{line[0], line[1]}); //url, branch
        }
        writer.close();
    }

    /**
     * Generates a new csv file (input/projects-input.csv) based on the one (input/projects.csv) that contains the
     * result of BigQuery engine, excluding duplicated lines and unnecessary columns.
     *
     * @throws IOException if there's an error reading or writting csv files.
     */
    public static void prepareInput() throws IOException {
        CSVReader reader = new CSVReader(new FileReader(SearchProperties.BIGQUERY_COMMITS_FILE));
        List<String[]> entries = reader.readAll();
        reader.close();

        List<String[]> unique = removeDuplicatedValues(entries);
        CSVWriter writer = new CSVWriter(new FileWriter(SearchProperties.REPOSITORIES_TO_DOWNLOAD_FILE));
        for (String[] line : unique) {
            writer.writeNext(new String[]{line[0], line[1]}); //url, branch
        }
        writer.close();
    }

    private static List<String[]> removeDuplicatedValues(List<String[]> input){
        List<String[]> output = new ArrayList<>();
        for (String[] line : input) {
            if(line[1].equals("null") || line[1].contains("java.lang.Object@")) line[1] = "master";
            String[] shortLine = Arrays.copyOf(line,2);
            if ( !contains(output,shortLine) ){
                output.add(shortLine);
            }
        }
        return output;
    }

    private static boolean contains(List<String[]> input, String[] line){
        boolean result = false;
        for (String[] s: input) {
            if(Arrays.equals(s,line)){
                result = true;
                break;
            }
        }
        return result;
    }

}
