package filter

import au.com.bytecode.opencsv.CSVWriter
import groovy.util.logging.Slf4j
import repositorySearch.SearchResultManager
import util.ConstantData
import util.CsvUtil
import util.DataProperties

@Slf4j
class RepositoryFilterManager {

    SearchResultManager resultManager
    int repositoriesCounter
    List<GitHubRepository> candidates
    File file

    RepositoryFilterManager() {
        candidates = []
        resultManager = new SearchResultManager()
    }

    private configureFile(){
        file = new File(ConstantData.CANDIDATE_REPOSITORIES_FILE)
        if(file.exists()) file.delete()
        List<String[]> content = []
        String[] header = ["INDEX", "URL", "STARS", "SIZE", "GEMS"]
        content += header
        CsvUtil.append(ConstantData.CANDIDATE_REPOSITORIES_FILE, content)
    }

    private resetCounters() {
        repositoriesCounter = 0
        candidates.clear()
    }

    private printCounter() {
        log.info "Found projects: ${repositoriesCounter}"
        if (repositoriesCounter > 0) {
            def value = ((double) candidates.size() / repositoriesCounter) * 100
            log.info "Filtered projects: ${candidates.size()} (${value}%%)"
        } else
            log.info "Filtered projects: ${candidates.size()} (0%%)"
    }

    private listCandidateRepositories() {
        for (GitHubRepository r : candidates) {
            log.info "url: ${r.getUrl()}, branch: ${r.getBranch()}, zip: ${r.getLocalZipName()}"
        }
    }

    private void filterRepositories(GitHubRepository repository, int index) {
        if (repository.satisfiesFilteringCriteria()) {
            log.info "${repository.url} satisfies filtering criteria!"
            candidates.add(repository)
            String[] args = [String.valueOf(index), repository.url, repository.stars, repository.size, DataProperties.GEMS]
            CSVWriter writer = new CSVWriter(new FileWriter(file, true))
            writer.writeNext(args)
            writer.close()
        } else {
            log.info "${repository.url} does not satisfy filtering criteria!"
            repository.deleteAll()
        }
    }

    def searchRepositoriesByFileTypeAndGems() {
        CSVWriter writer = null
        List<GitHubRepository> repositories
        resetCounters()
        try {
            configureFile()
            repositories = resultManager.extractRepositoriesFromSearchResult()
            repositoriesCounter = repositories.size()
            for (int i = 0; i < repositories.size(); i++) {
                filterRepositories(repositories.get(i), i+1)
            }
            printCounter()
            listCandidateRepositories()
        } catch (IOException e) {
            log.error "Error while filtering repositories by file type and gems."
            e.stackTrace.each{ log.error it.toString() }
        } finally {
            writer?.close()
        }
    }

}