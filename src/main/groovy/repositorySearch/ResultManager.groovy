package repositorySearch

import com.google.api.services.bigquery.model.TableCell
import com.google.api.services.bigquery.model.TableRow
import filter.GitHubRepository
import groovy.util.logging.Slf4j
import util.ConstantData
import util.CsvUtil

@Slf4j
class ResultManager {

    String inputFile
    String outputFile

    ResultManager(){
        inputFile = ConstantData.BIGQUERY_COMMITS_FILE
        outputFile = ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE
    }

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

    def saveQueryResult(List<TableRow> rows) throws IOException {
        List<String[]> content = []
        String[] param = ["URL", "MASTER_BRANCH", "COMMIT_MSG", "COMMIT_LINK", "COMMIT_ID", "CREATED_AT", "WATCHERS"]
        content += param
        if (rows != null) {
            for (TableRow row : rows) {
                List<TableCell> fields = row.getF()
                String[] entry = new String[fields.size()] //url, branch, msg, commit link, commit id, date, watchers
                for (int i = 0; i < entry.length; i++) {
                    entry[i] = fields.get(i).getV().toString()
                }
                content += entry
            }
        } else {
            log.info "No repository was found!"
        }

        CsvUtil.write(inputFile, content)
    }

    List<GitHubRepository> extractRepositoriesFromSearchResult() throws IOException {
        List<GitHubRepository> repos = []
        List<String[]> entries = CsvUtil.read(outputFile)
        if (entries.size() > 0) entries.remove(0) //ignore sheet header
        for (String[] line : entries) {
            repos.add(new GitHubRepository(line[0], line[1], line[2], line[3] as int, line[4] as int, line[5]))
        }
        repos
    }

    def generateRepositoriesCsv() throws IOException {
        List<String[]> entries = CsvUtil.read(inputFile)
        List<String[]> unique = uniqueValues(entries)
        unique.remove(0)
        List<String[]> content = []
        String[] header = ["URL", "MASTER_BRANCH", "CREATED_AT", "STARS", "SIZE", "DESCRIPTION"]
        content += header
        for (String[] line : unique) {
            String[] args = [line[0], line[1], "", "", "", ""] //url, branch
            content += args
        }
        CsvUtil.write(outputFile, content)
    }

}
