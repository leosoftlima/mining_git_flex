package repositorySearch

import au.com.bytecode.opencsv.CSVReader
import au.com.bytecode.opencsv.CSVWriter
import util.DataProperties

class RepositoriesCsvOrganizer {

    private static List<String[]> uniqueValues(List<String[]> input) {
        List<String[]> output = new ArrayList<>()
        for (String[] line : input) {
            if (line[1] == "null" || line[1].contains("java.lang.Object@")) line[1] = "master"
            String[] shortLine = Arrays.copyOf(line, 2)
            if (!contains(output, shortLine)) {
                output.add(shortLine)
            }
        }
        output
    }

    private static boolean contains(List<String[]> input, String[] line) {
        boolean result = false
        for (String[] s : input) {
            if (Arrays.equals(s, line)) {
                result = true
                break
            }
        }
        result
    }

    /**
     * Generates a new csv file (input/projects-input.csv) based on the one that contains the result of BigQuery engine,
     * excluding duplicated lines and unnecessary columns.
     *
     * @param csvPath csv file that contains the result of BigQuery engine (repository url and master branch)
     * @throws IOException if there's an error reading or writting csv files
     */
    private static prepareInput() throws IOException {
        CSVReader reader = new CSVReader(new FileReader(DataProperties.BIGQUERY_COMMITS_FILE))
        List<String[]> entries = reader.readAll()
        reader.close()

        List<String[]> unique = uniqueValues(entries)
        CSVWriter writer = new CSVWriter(new FileWriter(DataProperties.REPOSITORIES_TO_DOWNLOAD_FILE))
        for (String[] line : unique) {
            String[] args = [line[0], line[1]]
            writer.writeNext(args) //url, branch
        }
        writer.close()
    }

    private static extractRepoEntries(){
        prepareInput()
        CSVReader reader = new CSVReader(new FileReader(DataProperties.REPOSITORIES_TO_DOWNLOAD_FILE))
        List<String[]> entries = reader.readAll()
        reader.close()
        entries
    }

    static List<GitHubRepository> extractRepositories() throws IOException {
        List<GitHubRepository> repos = []
        List<String[]> entries = extractRepoEntries()
        if (entries.size() > 0) entries.remove(0) //ignore sheet header
        for (String[] line : entries) {
            repos.add(new GitHubRepository(line[0], line[1])) //url, branch
        }
        repos
    }

}
