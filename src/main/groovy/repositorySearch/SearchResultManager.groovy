package repositorySearch

import au.com.bytecode.opencsv.CSVReader
import au.com.bytecode.opencsv.CSVWriter
import filter.GitHubRepository
import util.ConstantData

class SearchResultManager {

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

    private static extractRepoEntries(){
        CSVReader reader = new CSVReader(new FileReader(ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE))
        List<String[]> entries = reader.readAll()
        reader.close()
        entries
    }

    static List<GitHubRepository> extractRepositories() throws IOException {
        List<GitHubRepository> repos = []
        List<String[]> entries = extractRepoEntries()
        if (entries.size() > 0) entries.remove(0) //ignore sheet header
        for (String[] line : entries) {
            repos.add(new GitHubRepository(line[0], line[1], line[3] as int, line[4] as int))
        }
        repos
    }

    static generateRepositoriesCsv() throws IOException {
        CSVReader reader = new CSVReader(new FileReader(ConstantData.BIGQUERY_COMMITS_FILE))
        List<String[]> entries = reader.readAll()
        reader.close()

        List<String[]> unique = uniqueValues(entries)
        CSVWriter writer = new CSVWriter(new FileWriter(ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE))
        for (String[] line : unique) {
            String[] args = [line[0], line[1]]
            writer.writeNext(args) //url, branch
        }
        writer.close()
    }

}
