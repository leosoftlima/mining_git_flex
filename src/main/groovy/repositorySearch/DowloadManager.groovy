package repositorySearch

import au.com.bytecode.opencsv.CSVWriter
import groovy.util.logging.Slf4j
import util.ConstantData

@Slf4j
class DowloadManager {

    int counter
    final ArrayList<GitHubRepository> candidates

    DowloadManager() {
        candidates = new ArrayList<>()
    }

    private resetCounters() {
        counter = 0
        candidates.clear()
    }

    private printCounter() {
        log.info "Found projects: ${counter}"
        if (counter > 0) log.info "Filtered projects: ${candidates.size()} (${((double) candidates.size() / counter) * 100}%%)"
    }

    private listCandidateRepositories() {
        for (GitHubRepository r : candidates) {
            log.info "url: ${r.getUrl()}, branch: ${r.getBranch()}, zip: ${r.getLocalZipName()}"
        }
    }

    private void filterRepositories(GitHubRepository repository, CSVWriter writer, int index) {
        if (repository.satisfiesFilteringCriteria()) {
            log.info "${repository.url} satisfies filtering criteria!"
            candidates.add(repository)
            String[] args = [String.valueOf(index), repository.getUrl()]
            writer.writeNext(args)
        } else {
            log.info "${repository.url} does not satisfy filtering criteria!"
            repository.deleteAll()
        }
        saveFileForCommitsSearch(candidates)
    }

    private static saveFileForCommitsSearch(List<GitHubRepository> repos){
        def file = new File(ConstantData.MERGE_ANALYSIS_TEMP_FILE)
        file.createNewFile()
        if(!repos || repos.empty) return
        file.withWriter("utf-8") { out ->
            repos.each { out.write it.name }
        }
    }

    def searchRepositoriesByFileTypeAndGems() {
        CSVWriter writer
        ArrayList<GitHubRepository> repositories
        resetCounters()
        try {
            repositories = CsvOrganizer.extractRepositories()
            counter = repositories.size()
            def file = new File(ConstantData.CANDIDATE_REPOSITORIES_FILE)
            writer = new CSVWriter(new FileWriter(file))
            String[] header = ["INDEX", "URL"]
            writer.writeNext(header)
            for (int i = 0; i < repositories.size(); i++) {
                filterRepositories(repositories.get(i), writer, i + 1)
            }
            printCounter()
            writer.close()
            listCandidateRepositories()
        } catch (IOException e) {
            e.getStackTrace().each{ log.error it.toString() }
        }
    }

}
