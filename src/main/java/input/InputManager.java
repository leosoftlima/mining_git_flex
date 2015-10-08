package input;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import util.Util;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class InputManager {

    public static void prepareInput(String csvName) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(csvName));
        List<String[]> entries = reader.readAll();
        reader.close();

        List<String[]> unique = removeDuplicatedValues(entries);
        CSVWriter writer = new CSVWriter(new FileWriter(Util.PREPARED_PROJECTS_FILE));
        for (String[] line : unique) {
            writer.writeNext(new String[]{line[0], line[1]}); //url, branch
        }
        writer.close();
    }

    public static void prepareInput() throws IOException {
        CSVReader reader = new CSVReader(new FileReader(Util.PROJECTS_FILE));
        List<String[]> entries = reader.readAll();
        reader.close();

        List<String[]> unique = removeDuplicatedValues(entries);
        CSVWriter writer = new CSVWriter(new FileWriter(Util.PREPARED_PROJECTS_FILE));
        for (String[] line : unique) {
            writer.writeNext(new String[]{line[0], line[1]}); //url, branch
        }
        writer.close();
    }

    private static List<String[]> removeDuplicatedValues(List<String[]> input){
        List<String[]> output = new ArrayList<>();
        for (String[] line : input) {
            if(line[1].equals("null")) line[1] = "master";
            if (!contains(output, line)){
                output.add(line);
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
