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
    String problematicMergesCsv

    String tasksCsv
    String mergeTasksFile
    String uniqueTasksFile
    String independentTasksFile
    String ptTasksFile

    private static int taskId
    GitRepository repository
    List<MergeScenario> mergeScenarios
    List<String> problematicMerges
    List<String> fastForwardMerges
    List<MergeTask> tasks
    List<MergeTask> ptTasks
    List<MergeTask> uniqueTasks
    List<MergeTask> independentTasks

    MergeTaskExtractor(String mergeFile) throws Exception {
        taskId = 0
        this.mergesCsv = mergeFile
        this.problematicMergesCsv = mergeFile - ConstantData.MERGE_TASK_SUFIX + ConstantData.PROBLEMATIC_MERGE_TASK_SUFIX
        this.fastforwardMergesCsv = mergeFile - ConstantData.MERGE_TASK_SUFIX + ConstantData.FASTFORWARD_MERGE_TASK_SUFIX
        this.fastForwardMerges = CsvUtil.read(fastforwardMergesCsv).collect{ it[0] }
        this.problematicMerges = CsvUtil.read(problematicMergesCsv).collect{ it[0] }
        this.mergeScenarios = extractMergeScenarios()
        if(this.mergeScenarios.empty) throw new Exception("No merge commit was found!")

        def url = mergeScenarios.first().url
        this.repository = GitRepository.getRepository(url)

        this.tasksCsv = "${ConstantData.TASKS_FOLDER}${repository.name}.csv"
        this.mergeTasksFile = "${ConstantData.TASKS_FOLDER}${repository.name}-merge-tasks.csv"
        this.uniqueTasksFile = "${ConstantData.TASKS_FOLDER}${repository.name}-unique-tasks.csv"
        this.independentTasksFile = "${ConstantData.TASKS_FOLDER}${repository.name}-independent-tasks.csv"
        this.ptTasksFile = "${ConstantData.TASKS_FOLDER}${repository.name}-pt-tasks.csv"

        this.tasks = []
        this.ptTasks = []
        this.uniqueTasks = []
        this.independentTasks = []
    }

    private static printMergeInfo(MergeScenario merge){
        log.info "Extracting merge scenario"
        log.info "merge commit: ${merge.merge}"
        log.info "base commit: ${merge.base}"
        log.info "left commit: ${merge.left}"
        log.info "right commit: ${merge.right}\n"
    }

    private extractFirstTaskFromPartialMergeScenario(List<Commit> mergeCommits, List<Commit> commits, MergeScenario merge){
        def task = null
        def mergeCommit = merge.merge
        def baseCommit = mergeCommits.first()
        log.info "Merge pairs: [${mergeCommit}, ${baseCommit.hash}]"
        def index = commits.indexOf(baseCommit)
        def commitsOfInterest = commits.subList(0, index)
        if(!commitsOfInterest.empty) {
            task = new MergeTask(repository.url, ++taskId as String, commitsOfInterest, mergeCommit,
                    baseCommit.hash, commitsOfInterest.first().hash)
        }
        task
    }

    private extractTasksWhenThereIsOnlyOneIntermediateMerge(Commit intermediateMerge, List<Commit> commits,
                                                            MergeScenario merge){
        def tasks = []
        if(commits.size()> 1){ //if there only 1 commit and it is a merge, we cannot extract any task
            def index = commits.indexOf(intermediateMerge)
            def  set1 = commits.subList(0, index) //task between merge.merge and intermediateMerge
            if(!set1.empty){ //if the merge is not the first commit, there is a valid commit set
                tasks += new MergeTask(repository.url, ++taskId as String, set1, merge.merge, intermediateMerge.hash,
                        set1.first().hash)
            }
        }
        tasks
    }

    private extractTasksFromIntermediateMerge(MergeScenario originalMergeScenario, List<Commit> commitsSetFromBranch){
        def mergeCommits = commitsSetFromBranch.findAll{ it.isMerge && !isFastForwardMerge(it) }
        List<MergeTask> result = []
        if(mergeCommits.size()==1){ //only 1 merge
            log.info "There is only 1 intermediate merge: ${mergeCommits.get(0).hash}"
            log.info "Merge pairs: [${originalMergeScenario.merge}, ${mergeCommits.get(0).hash}]"
            result += extractTasksWhenThereIsOnlyOneIntermediateMerge(mergeCommits.get(0), commitsSetFromBranch,
                    originalMergeScenario)
        } else{ //multiple merges
            log.info "There is multiple intermediate merges: ${mergeCommits.size()}"

            /* commits set between merge and the first intermediate merge */
            def task = extractFirstTaskFromPartialMergeScenario(mergeCommits, commitsSetFromBranch, originalMergeScenario)
            if(task) result += task
        }
        result
    }

    private isFastForwardMerge(Commit commit){
        def found = fastForwardMerges.find{ it == commit.hash }
        !(found == null)
    }

    private isProblematicMerge(Commit commit){
        def found = problematicMerges.find{ it == commit.hash }
        !(found == null)
    }

    private extractTaskFromMergeBranch(MergeScenario mergeScenario, List<String> commitsFromBranch, String hash){
        List<MergeTask> result = []
        List<Commit> commitsSetFromBranch = repository?.searchCommits(commitsFromBranch)

        def mergeCommitsSet = commitsSetFromBranch.findAll{ it.isMerge && !isFastForwardMerge(it) }
        log.info "Extracting task from a branch of merge: ${mergeScenario.merge}"
        if(!commitsSetFromBranch.empty && mergeCommitsSet.empty){ //there is no intermediate merges
            def task = new MergeTask(repository.url, ++taskId as String, commitsSetFromBranch, mergeScenario, hash)
            result += task
            log.info "There is no intermediate merges"
        } else if(!commitsSetFromBranch.empty && !mergeCommitsSet.empty){ //there is intermediate merges
            log.info "There is ${mergeCommitsSet.size()} intermediate merges"
            mergeCommitsSet.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }
            log.info "There is ${commitsSetFromBranch.size()} commits"
            commitsSetFromBranch.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }
            result += extractTasksFromIntermediateMerge(mergeScenario, commitsSetFromBranch)
        }
        result
    }

    private configureMergeTask(MergeScenario mergeScenario){
        printMergeInfo(mergeScenario)

        List<MergeTask> result
        List<MergeTask> resultLeft
        List<MergeTask> resultRight

        // Left
        resultLeft = extractTaskFromMergeBranch(mergeScenario, mergeScenario.leftCommits, mergeScenario.left)
        log.info "Tasks from left side: ${resultLeft.size()}"
        resultLeft.each{ log.info it.toString() }

        // Right
        resultRight = extractTaskFromMergeBranch(mergeScenario, mergeScenario.rightCommits, mergeScenario.right)
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
            url = entries[0][0]
            entries = entries.subList(2, entries.size())
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

    private filterUniqueTasks(){
        uniqueTasks = tasks.unique{ it.commits }
        uniqueTasks = uniqueTasks.unique{ [it.repositoryUrl, it.newestCommit, it.merge, it.base] }
        uniqueTasks = uniqueTasks.unique{ [it.repositoryUrl, it.id] }.sort{ it.id }
    }

    private filterProductionAndTestTasks(){
        ptTasks = uniqueTasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
    }

    private filterIndependentTasks(){
        independentTasks = []
        def hashesSimilarity = computeHashSimilarity() //["task_a","hashes_a","task_b","hashes_b","intersection","%_a","%_b"]
        if(hashesSimilarity.empty && !ptTasks.empty) {
            independentTasks = ptTasks
            return
        } else if (hashesSimilarity.empty) return

        def maxSimResult = []
        def pairsMaxSimilarity = hashesSimilarity.findAll{ (it[5]==1) || (it[6]==1) }
        def ids = ptTasks.collect{ it.id }
        ids.each{ id ->
            def n = pairsMaxSimilarity.findAll{ it[0]==id || it[2]==id }
            if(n.size()>0) {
                def temp = []
                n.each{ pair ->
                    if(pair[1]==pair[3]){
                        def commitsNumber = pair[1]
                        def idPair = [pair[0] as int, pair[2] as int].sort()
                        temp.add(ptTasks.find{ it.id == (idPair[0] as String) && it.commits.size() == commitsNumber })
                    } else if(pair[0]==id && pair[1]>pair[3] ){
                        temp.add(ptTasks.find{ it.id == pair[0] })
                    } else if(pair[2]==id && pair[3]>pair[1]){
                        temp.add(ptTasks.find{ it.id == pair[2] })
                    }
                }
                maxSimResult += temp
            }
        }
        maxSimResult = maxSimResult.unique()
        independentTasks = (ptTasks - maxSimResult).sort{ it.id as double }
    }

    private computeHashSimilarity(){
        def hashesSimilarity = []
        def taskPairs = computeTaskPairs(ptTasks)
        if(taskPairs.empty) return hashesSimilarity
        taskPairs?.each { item ->
            def task = item.task
            def hashes1 = task.commits*.hash
            item.pairs?.each { other ->
                def hashes2 = other.commits*.hash
                def intersection = hashes1.intersect(hashes2).size()
                hashesSimilarity.add([task.id, hashes1.size(), other.id, hashes2.size(), intersection,
                                      intersection/hashes1.size(), intersection/hashes2.size()])
            }
        }
        hashesSimilarity
    }

    private static computeTaskPairs(List<MergeTask> set) {
        def result = [] as Set
        if (!set || set.empty || set.size() == 1) return result
        set.eachWithIndex { v, k ->
            def next = set.drop(k + 1)
            result.add([task: v, pairs: next])
        }
        result
    }

    def extractTasks(){
        tasks = []
        mergeScenarios?.each{ tasks += configureMergeTask(it) }
        log.info "Found merge tasks: ${tasks.size()}"
        Util.exportTasksWithConflictInfo(tasks, mergeTasksFile)

        filterUniqueTasks()
        log.info "Found unique tasks (from merge tasks): ${uniqueTasks.size()}"
        Util.exportTasksWithConflictInfo(uniqueTasks, uniqueTasksFile)

        if(DataProperties.SEARCH_PT_TASKS){
            filterProductionAndTestTasks()
            log.info "Found P&T tasks (from unique tasks): ${ptTasks.size()}"
            Util.exportTasksWithConflictInfo(ptTasks, ptTasksFile)

            filterIndependentTasks()
            log.info "Found independent tasks (from P&T tasks): ${independentTasks.size()}"
            Util.exportTasksWithConflictInfo(independentTasks, independentTasksFile)
        }
    }

}
