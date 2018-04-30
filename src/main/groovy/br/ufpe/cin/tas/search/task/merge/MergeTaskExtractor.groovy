package br.ufpe.cin.tas.search.task.merge

import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.search.task.GitRepository
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.CsvUtil
import br.ufpe.cin.tas.util.Util

@Slf4j
class MergeTaskExtractor {

    String mergesCsv
    GitRepository repository
    List<MergeScenario> mergeScenarios
    String tasksCsv
    String cucumberConflictingTasksCsv
    private static int taskId

    MergeTaskExtractor(String mergeFile) throws Exception {
        taskId = 0
        mergesCsv = mergeFile
        mergeScenarios = extractMergeScenarios()
        if(mergeScenarios.empty){
            throw new Exception("No merge commit was found!")
        }
        def url = mergeScenarios.first().url
        repository = GitRepository.getRepository(url)
        tasksCsv = "${ConstantData.TASKS_FOLDER}${repository.name}.csv"
        cucumberConflictingTasksCsv = "${ConstantData.TASKS_FOLDER}${repository.name}-conflict.csv"
    }

    private static printMergeInfo(MergeScenario merge){
        log.info "Extracting merge scenario"
        log.info "merge commit: ${merge.merge}"
        log.info "base commit: ${merge.base}"
        log.info "left commit: ${merge.left}"
        log.info "right commit: ${merge.right}\n"
    }

    private configureMergeTask(MergeScenario merge){
        printMergeInfo(merge)

        def commits1 = repository?.searchCommits(merge.leftCommits)
        def commits2 = repository?.searchCommits(merge.rightCommits)
        if(!commits1.empty && !commits2.empty){
            def leftTask = new MergeTask(repository.url, ++taskId as String, commits1, merge, merge.left)
            def rightTask = new MergeTask(repository.url, ++taskId as String, commits2, merge, merge.right)
            return [leftTask, rightTask]
        } else return []
    }

    private configureMergeTaskWithConflictInfo(MergeScenario merge){
        printMergeInfo(merge)
        def result = []
        def commits1 = repository?.searchCommits(merge.leftCommits)
        def commits2 = repository?.searchCommits(merge.rightCommits)
        if(!commits1.empty && !commits2.empty){
            List conflictingFiles = repository.extractConflictingFiles(merge)
            if(conflictingFiles!=null){
                def leftTask = new MergeTask(repository.url, ++taskId as String, commits1, merge, merge.left, conflictingFiles)
                def rightTask = new MergeTask(repository.url, ++taskId as String, commits2, merge, merge.right, conflictingFiles)
                result = [leftTask, rightTask]
            }
        }
        result
    }

    private List<MergeScenario> extractMergeScenarios(){
        def merges = []
        def url = ""
        List<String[]> entries = CsvUtil.read(mergesCsv)
        if (entries.size() > 2){
            url = entries.first()[0]
            entries.removeAt(0)
            entries.removeAt(0)
            entries?.each{ entry ->
                def v1, v2
                if(entry[4].size()>2) v1 = entry[4].substring(1, entry[4].size()-1).tokenize(', ')
                else v1 = []
                if(entry[5].size()>2) v2 = entry[5].substring(1, entry[5].size()-1).tokenize(', ')
                else v2 = []
                merges += new MergeScenario(url:url, merge:entry[0], left:entry[1], right:entry[2], base:entry[3],
                        leftCommits: v1 as List<String>, rightCommits: v2 as List<String>)
            }
        }
        merges
    }

    def extractTasks(){
        def tasks = []
        mergeScenarios?.each{ tasks += configureMergeTask(it) }
        def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
        log.info "Found merge tasks: ${tasks.size()}"
        log.info "Found P&T tasks: ${tasksPT.size()}"

        def taskGroups = tasksPT.groupBy { it.newestCommit }
        log.info "SHAs: ${taskGroups.size()}"
        taskGroups.eachWithIndex{ group, index ->
            def sha = group.key as String
            def gems = extractGemsInfo(sha)
            log.info "${index} Extracted gems for commit '${sha}'"
            group.getValue().each{ task -> task.gems = gems }
        }
        Util.exportProjectTasks(tasksPT, tasksCsv, repository.url)
    }
    
    def extractGemsInfo(String sha){
        repository.clean()
        repository.reset(sha)
        repository.checkout(sha)
        def gems = Util.checkRailsVersionAndGems(repository.getLocalPath())
        repository.reset()
        repository.checkout()
        gems
    }

    def extractCucumberConflictingTasks(){
        List<MergeTask> tasks = []
        mergeScenarios?.each{ tasks += configureMergeTaskWithConflictInfo(it) }
        def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
        def taskGroups = tasksPT.groupBy { it.newestCommit }
        log.info "SHAs: ${taskGroups.size()}"
        taskGroups.eachWithIndex{ group, index ->
            def sha = group.key as String
            def gems = extractGemsInfo(sha)
            log.info "${index} Extracted gems for commit '${sha}'"
            group.getValue().each{ task -> task.gems = gems }
        }
        def cucumberTasks = tasksPT.findAll{ it.hasTests() }
        Util.exportTasksWithConflictInfo(cucumberTasks, cucumberConflictingTasksCsv)
    }

}
