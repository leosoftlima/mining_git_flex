package br.ufpe.cin.tas.search.task.id

import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.search.task.GitRepository
import br.ufpe.cin.tas.search.task.Task
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.DataProperties
import br.ufpe.cin.tas.util.RegexUtil
import br.ufpe.cin.tas.util.Util

@Slf4j
class IdTaskExtractor {

    GitRepository repository
    List<Commit> commits
    String tasksCsv

    IdTaskExtractor(String url) throws Exception {
        repository = GitRepository.getRepository(url)
        if(repository == null) throw new Exception("Error: Repository '$url' not found.")
        if (DataProperties.FILTER_BY_PIVOTAL_TRACKER)
            commits = repository?.searchByComment(RegexUtil.PIVOTAL_TRACKER_ID_REGEX)
        else if (DataProperties.FILTER_BY_DEFAULT_MESSAGE)
            commits = repository?.searchByComment(RegexUtil.GENERAL_ID_REGEX)
        tasksCsv = "${ConstantData.TASKS_FOLDER}${repository?.name}.csv"
    }

    private organizeCommitsById(){
        def organizedCommits = []
        commits?.each { commit ->
            def idList = commit.extractIds()
            organizedCommits += [commit: commit, code: idList]
        }
        organizedCommits
    }

    def extractTasks() {
        def tasks = []
        def organizedCommits = organizeCommitsById()
        def ids = (organizedCommits*.code)?.unique()?.flatten()
        ids?.each { id ->
            def commitsWithId = organizedCommits.findAll { id in it.code }
            List<Commit> taskCommits = commitsWithId*.commit
            def newestCommit = repository.findNewestCommit(taskCommits)
            Task task = new Task(repository.url, id, taskCommits, newestCommit)
            tasks += task
        }
        Util.exportProjectTasks(tasks, tasksCsv, repository.url)
    }

}
