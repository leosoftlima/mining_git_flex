package repositorySearch

import au.com.bytecode.opencsv.CSVWriter
import groovy.util.logging.Slf4j
import util.DataProperties

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
        log.info "Number of analyzed projects: ${counter}"
        if (counter > 0) log.info "Number of candidates projects: ${candidates.size()} (${((double) candidates.size() / counter) * 100}%%)"
    }

    private listCandidateRepositories() {
        for (GitHubRepository r : candidates) {
            log.info "url: ${r.getUrl()}, branch: ${r.getBranch()}, zip: ${r.getLocalZipName()}"
        }
    }

    /**
     * Verifies if the repository does contain a file type and updates a csv file to include the analysed repository if
     * the result is positive.
     *
     * @param repository the repository to analyse.
     * @param writer cvs file to update.
     * @param index original index of the analysed repository.
     */
    private void searchFileType(GitHubRepository repository, CSVWriter writer, int index) {
        counter++
        if (repository.hasFileType(DataProperties.FILE_TYPE)) {
            candidates.add(repository)
            repository.deleteUnzipedDir()
            String[] args = [String.valueOf(index), repository.getUrl()]
            writer.writeNext(args)
        } else {
            repository.deleteAll()
        }
    }

    /**
     * Verifies if the GitHub repositories identified at a csv file (/input/commits.csv) does contain files of a specific type.
     * The first column must identify the repository's url and the second column must identify its master branch.
     *
     */
    def searchRepositoriesByFileType() {
        CSVWriter writer
        ArrayList<GitHubRepository> repositories
        resetCounters()

        try {
            repositories = RepositoriesCsvOrganizer.extractRepositories()
            log.info "The repositories to search for are saved in ${DataProperties.REPOSITORIES_TO_DOWNLOAD_FILE}"

            def file = new File(DataProperties.CANDIDATE_REPOSITORIES_FILE)
            writer = new CSVWriter(new FileWriter(file))
            String[] args1 = ["index", "repository_url"]
            writer.writeNext(args1)

            if (DataProperties.FILE_TYPE.empty) {
                for (int i = 0; i < repositories.size(); i++) {
                    candidates.add(repositories.get(i))
                    String[] args2 = [String.valueOf(i + 2), repositories.get(i).getUrl()]
                    writer.writeNext(args2)
                }
            } else {
                for (int i = 0; i < repositories.size(); i++) {
                    searchFileType(repositories.get(i), writer, i + 2)
                }
                printCounter()
            }

            writer.close()
            listCandidateRepositories()
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

}
