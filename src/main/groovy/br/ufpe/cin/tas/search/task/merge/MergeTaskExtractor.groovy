package br.ufpe.cin.tas.search.task.merge

import br.ufpe.cin.tas.search.task.id.Commit
import br.ufpe.cin.tas.util.DataProperties
import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.search.task.GitRepository
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.CsvUtil
import br.ufpe.cin.tas.util.Util

@Slf4j
class MergeTaskExtractor {

    String mergesCsv
    String fastforwardMergesCsv
    GitRepository repository
    List<MergeScenario> mergeScenarios
    String tasksCsv
    String cucumberConflictingTasksCsv
    private static int taskId
    List<String> fastForwardMerges

    MergeTaskExtractor(String mergeFile) throws Exception {
        taskId = 0
        mergesCsv = mergeFile
        fastforwardMergesCsv = mergeFile - ConstantData.MERGE_TASK_SUFIX + ConstantData.FASTFORWARD_MERGE_TASK_SUFIX
        this.fastForwardMerges = CsvUtil.read(fastforwardMergesCsv).collect{ it[0] }
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

    private extractFirstTaskFromPartialMergeScenario(List<Commit> mergeCommits1, List<Commit> commits1, MergeScenario merge){
        def intermediateMerge = mergeCommits1.get(0)
        extractTask(intermediateMerge, commits1, merge)
    }

    private extractLastTaskFromPartialMergeScenario(List<Commit> mergeCommits1, List<Commit> commits1, MergeScenario merge){
        def leftTask = null
        def mergeCommit = mergeCommits1.last()
        def baseCommit = merge.base
        log.info "Merge pairs: [${mergeCommit.hash}, ${baseCommit}]"
        def index1 = commits1.indexOf(mergeCommit)
        def commitsOfInterest = commits1.subList(index1+1, commits1.size())
        if(!commitsOfInterest.empty && !(commitsOfInterest.size()==1 && commitsOfInterest.get(0).isMerge) ) {
            leftTask = new MergeTask(repository.url, ++taskId as String, commitsOfInterest, mergeCommit.hash,
                    baseCommit, commitsOfInterest.first().hash)
        }
        leftTask
    }

    private extractTask(Commit intermediateMerge, List<Commit> commits1, MergeScenario merge){
        def leftTask = null
        def index = commits1.indexOf(intermediateMerge)
        def commitsOfInterest = commits1.subList(0, index)
        if(commitsOfInterest.empty){ //there is only an intermediate merge
            def found = fastForwardMerges.find{ it == intermediateMerge.hash }
            if(found){
                log.info "Intermediate merge is fast-forward: ${intermediateMerge.hash}"
                leftTask = new MergeTask(repository.url, ++taskId as String, [intermediateMerge], merge.merge, merge.base, merge.left)
            } else log.info "Intermediate merge is not fast-forward: ${intermediateMerge.hash}"
        } else if(!(commitsOfInterest.size()==1 && commitsOfInterest.get(0).isMerge) ) {
            leftTask = new MergeTask(repository.url, ++taskId as String, commitsOfInterest, merge.merge,
                    intermediateMerge.hash, merge.left)
        }
        leftTask
    }

    private extractTasksFromPartialMergeScenario(List<Commit> mergeCommits, List<Commit> commits, MergeScenario merge){
        List<MergeTask> result = []
        if(mergeCommits.size()==1){ //only 1 merge
            log.info "There is only 1 merge: ${mergeCommits.get(0).hash}"
            log.info "Merge pairs: [${merge.merge}, ${mergeCommits.get(0).hash}]"
            def task = extractFirstTaskFromPartialMergeScenario(mergeCommits, commits, merge)
            if(task) result += task
        } else{ //multiple merges
            /* commits set between merge and left */
            def task = extractFirstTaskFromPartialMergeScenario(mergeCommits, commits, merge)
            log.info "Merge pairs: [${merge.merge}, ${mergeCommits.get(0).hash}]"
            if(task) result += task

            /* commits set between merges */
            def pairs = mergeCommits.collate(2, 1, false)
            pairs.each{ pair ->
                log.info "Merge pairs: ${pair*.hash}"
                def index1 = commits.indexOf(pair.get(0))
                def index2 = commits.indexOf(pair.get(1))
                def commitsOfInterest = commits.subList(index1+1, index2)
                if(!commitsOfInterest.empty && !(commitsOfInterest.size()==1 && commitsOfInterest.get(0).isMerge) ) {
                    task = new MergeTask(repository.url, ++taskId as String, commitsOfInterest, pair.get(0).hash,
                            pair.get(1).hash, commitsOfInterest.first().hash)
                    result += task
                }
            }

            /* commits set between last intermediate merge and base */
            task = extractLastTaskFromPartialMergeScenario(mergeCommits, commits, merge)
            if(task) result += task
        }
        result
    }

    private configureMergeTask(MergeScenario merge){
        printMergeInfo(merge)

        List<MergeTask> result
        List<MergeTask> resultLeft = []
        List<MergeTask> resultRight = []
        def commits1 = repository?.searchCommits(merge.leftCommits)
        def mergeCommits1 = commits1.findAll{ it.isMerge }
        def commits2 = repository?.searchCommits(merge.rightCommits)
        def mergeCommits2 = commits2.findAll{ it.isMerge }

        // Left
        log.info "Extracting left tasks from merge: ${merge.merge}"
        if(!commits1.empty && mergeCommits1.empty){ //there is no intermediate merges
            def leftTask = new MergeTask(repository.url, ++taskId as String, commits1, merge, merge.left)
            resultLeft += leftTask
            log.info "Left side does not have intermediate merges"
            log.info leftTask.toString()
        } else if(!commits1.empty && !mergeCommits1.empty){ //there is intermediate merges
            log.info "Left side has intermediate merges: ${mergeCommits1.size()}"
            mergeCommits1.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }
            log.info "Left side has commits: ${commits1.size()}"
            commits1.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }
            resultLeft += extractTasksFromPartialMergeScenario(mergeCommits1, commits1, merge)
        }
        log.info "Tasks from left side: ${resultLeft.size()}"
        resultLeft.each{ log.info it.toString() }

        // Right
        log.info "Extracting right tasks from merge: ${merge.merge}"
        if(!commits2.empty && mergeCommits2.empty){ //there is no intermediate merges
            def rightTask = new MergeTask(repository.url, ++taskId as String, commits2, merge, merge.right)
            resultRight += rightTask
            log.info "Right side does not have intermediate merges"
            log.info rightTask.toString()
        } else if(!commits2.empty && !mergeCommits2.empty){ //there is intermediate merges
            log.info "Right side has intermediate merges: ${mergeCommits2.size()}"
            mergeCommits2.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }
            log.info "Right side has commits:  ${commits2.size()}"
            commits2.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }
            resultRight += extractTasksFromPartialMergeScenario(mergeCommits2, commits2, merge)
        }
        log.info "Tasks from right side: ${resultRight.size()}"
        resultRight.each{ log.info it.toString() }

        result = (resultLeft + resultRight)?.unique()
        log.info "Final tasks number: ${result.size()}"

        result.unique{ [it.repositoryUrl, it.commits, it.newestCommit, it.merge, it.base] }
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
        List<MergeTask> tasks = []
        mergeScenarios?.each{ tasks += configureMergeTask(it) }
        tasks = tasks.unique{ [it.repositoryUrl, it.newestCommit, it.merge, it.base] }
        tasks = tasks.unique{ [it.repositoryUrl, it.commits] }
        tasks = tasks.unique{ [it.repositoryUrl, it.id] }.sort{ it.id }
        log.info "Unique tasks: ${tasks.size()}"

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
        if(DataProperties.CONFLICT_ANALYSIS){
            def cucumberTasks = tasksPT.findAll{ it.usesCucumber() }
            Util.exportTasksWithConflictInfo(cucumberTasks, cucumberConflictingTasksCsv)
        }
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

}
