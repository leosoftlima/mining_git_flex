package br.ufpe.cin.tas.filter

import au.com.bytecode.opencsv.CSVWriter
import br.ufpe.cin.tas.search.repository.ResultManager
import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.CsvUtil
import br.ufpe.cin.tas.util.DataProperties

@Slf4j
class RepositoryFilterManager {

    ResultManager resultManager
    int repositoriesCounter
    List<GitHubRepository> candidates
    File file

    RepositoryFilterManager() {
        candidates = []
        resultManager = new ResultManager()
        configureRailsFile()
    }

    private static configureRailsFile(){
        def railsRepoFile = new File(ConstantData.RAILS_REPOSITORIES_FILE)
        if(railsRepoFile.exists()) {
            railsRepoFile.delete()
            String[] header = ["URL", "MASTER_BRANCH", "CREATED_AT", "STARS", "SIZE", "DESCRIPTION", "GEMS"]
            List<String[]> railsProjects = []
            railsProjects += header
            CsvUtil.write(ConstantData.RAILS_REPOSITORIES_FILE, railsProjects)
        }
    }

    private configureFile(){
        file = new File(ConstantData.CANDIDATE_REPOSITORIES_FILE)
        if(file.exists()) file.delete()
        List<String[]> content = []
        String[] header = ["URL", "MASTER_BRANCH", "CREATED_AT", "STARS", "SIZE", "DESCRIPTION", "GEMS"]
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

    private void filterRepositories(GitHubRepository repository) {
        if (repository.satisfiesFilteringCriteria()) {
            log.info "${repository.url} satisfies filtering criteria!"
            candidates.add(repository)
            String[] args = [repository.url, repository.branch, repository.createdAt, repository.stars, repository.size,
                             repository.description, DataProperties.GEMS]
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
            repositories.each{ filterRepositories(it) }
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
