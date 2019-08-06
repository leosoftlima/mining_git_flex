package br.ufpe.cin.tas.search.task

import br.ufpe.cin.tas.search.task.merge.MergeTask
import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.treewalk.TreeWalk
import br.ufpe.cin.tas.search.task.id.Commit
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.RegexUtil
import br.ufpe.cin.tas.util.Util
import java.util.regex.Matcher


@Slf4j
class GitRepository {

    static List<GitRepository> repositories = []
    List<Commit> commits
    String url
    String name
    String localPath
    String lastCommit
    String defaultBranch

    private GitRepository(String path) {
        commits = []
        if (path.startsWith("http")) extractDataFromRemoteRepository(path)
        else extractDataFromlocalRepository(path)
        searchCommits()
        lastCommit = commits.first().hash
        log.info "Project: ${name}"
        log.info "All commits from project: ${commits.size()}"
        defaultBranch = nameFromDefaultBranch
        log.info "Default branch: ${defaultBranch}"
    }

    def diff(String sha1, String sha2){
        //-M to deal with renaming and -U for unified format
        //A saída seria pegar no resultado do diff, todas as linhas que começam com @@, pois temos informação da área alterada
        ProcessBuilder builder = new ProcessBuilder("git", "diff", "-M", "-U", sha1, sha2)
        builder.directory(new File(localPath))
        Process process = builder.start()
        def lines = process.inputStream.readLines()
        process.inputStream.close()
        def result = lines.findAll{ it.startsWith("diff --git ") || it.startsWith("@@ ") }
        formatDiffOutput(result)
    }

    static formatDiffOutput(List<String> result){
        def changes = []
        def file
        def lines
        result.each{ line ->
            if(line.startsWith("diff --git ")){
                def index = line.lastIndexOf(" ")
                file = (line.substring(index+1)).substring(2)
                lines = null
            }
            else if(line.startsWith("@@ ")){
                //consideramos a nova versão do arquivo, mas para lidar com linhas removidas, teríamos que considerar a versão original
                /* Exemplo: @@ -11,6 +9,4 @@
                * Na versão original, temos 6 linhas, a contar da linha 11.
                * Na versão nova, a região equivalente possui 4 linhas, a contar da linha 9. Ou seja, 2 linhas foram removidas.
                * */
                def index1 = line.lastIndexOf("+") //aqui pegamos a hunk da nova versão; para pegar a da original, usar "-"
                def interval = line.substring(index1+1)
                def index2 = interval.indexOf(" ")
                interval = interval.substring(0, index2)
                def index3 = interval.indexOf(",")
                def n
                if(index3<0) {
                    index3 = interval.indexOf(" ")
                    n = 0 as int
                }
                def indexInit = (interval.substring(0, index3) as int) - 1
                if(n==null) n = interval.substring(index3+1) as int
                def indexEnd = indexInit - 1 + n
                lines = indexInit..indexEnd
            }
            if(file && lines) changes.add([file:file, lines:lines])
        }
        def grouped = changes.groupBy{ it.file }
        def finalResult = []
        grouped.each{
            def changedLines = (it.value*.lines).flatten()
            finalResult += [file: it.key, lines:changedLines.unique()]
        }
        finalResult
    }

    def checkoutBranch(String branch, String startpoint){
        ProcessBuilder builder = new ProcessBuilder("git", "checkout", "-f", "-B", branch, startpoint)
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        process.inputStream.readLines()
        process.inputStream.close()
    }

    def checkoutBranch(String branch){
        ProcessBuilder builder = new ProcessBuilder("git", "checkout", "-f", "-B", branch)
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        process.inputStream.readLines()
        process.inputStream.close()
    }

    def checkout(String sha) {
        ProcessBuilder builder = new ProcessBuilder("git", "checkout", "-f", sha)
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        process.inputStream.readLines()
        process.inputStream.close()
    }

    def checkout() {
        ProcessBuilder builder = new ProcessBuilder("git", "checkout", "-f", lastCommit)
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        process.inputStream.readLines()
        process.inputStream.close()
    }

    def clean() {
        ProcessBuilder builder = new ProcessBuilder("git", "clean", "-f", "-d", "-x")
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        process.inputStream.readLines()
        process.inputStream.close()
    }

    def reset(String sha){
        ProcessBuilder builder = new ProcessBuilder("git", "reset", "--hard", sha)
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        def lines = process.inputStream.readLines()
        process.inputStream.close()
        lines
    }

    def reset(){
        ProcessBuilder builder = new ProcessBuilder("git", "reset", "--hard")
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        process.inputStream.readLines()
        process.inputStream.close()
    }

    def resetToLastCommit(){
        ProcessBuilder builder = new ProcessBuilder("git", "reset", "--hard", lastCommit)
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        process.inputStream.readLines()
        process.inputStream.close()
    }

    def revertMerge(){
        ProcessBuilder builder = new ProcessBuilder("git", "merge", "--abort")
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        process.inputStream.readLines()
        process.inputStream.close()
    }

    def searchMergeCommits(){
        ProcessBuilder builder = new ProcessBuilder("git", "log", "--merges")
        builder.directory(new File(localPath))
        Process process = builder.start()
        def lines = process?.inputStream?.readLines()
        process?.inputStream?.close()
        def result = []
        lines?.eachWithIndex { line, index ->
            if (line.startsWith('commit')) {
                def merge = line.split(' ')[1]
                def nextLine = lines.get(index + 1)
                String[] data = nextLine.split(' ')
                def left = data[1]
                def right = data[2]
                result += [merge:merge, left:left, right:right]
            }
        }
        result
    }

    def findBase(String left, String right){
        ProcessBuilder builder = new ProcessBuilder("git", "merge-base", left, right)
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        def aux = process?.inputStream?.readLines()
        process?.inputStream?.close()
        if(!aux || aux.empty) return null
        else aux?.first()?.replaceAll(RegexUtil.NEW_LINE_REGEX,"")
    }

    def findBase(MergeTask task1, MergeTask task2){
        def left = task1.newestCommit
        def right = task2.newestCommit
        findBase(left, right)
    }

    def getCommitSetBetween(String base, String other){
        ProcessBuilder builder = new ProcessBuilder("git", "rev-list", "${base}..${other}")
        builder.directory(new File(localPath))
        Process process = builder.start()
        def commitSet = process?.inputStream?.readLines()
        process?.inputStream?.close()
        commitSet
    }

    def deleteBranch(String branch){
        ProcessBuilder builder = new ProcessBuilder("git", "branch", "-D", branch)
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.waitFor()
        process.inputStream.readLines()
        process.inputStream.close()
    }
    
    def searchCommits(List hashes){
        if(hashes && !hashes.empty) {
            def result = []
            hashes.each{ hash ->
                def c = commits.find{ it.hash == hash }
                if(c) result += c
            }
            result
        }
        else []
    }

    def searchCommits() {
        if(commits.empty){
            def revCommits = searchAllRevCommits()
            revCommits?.each { c ->
                def files = getAllChangedFilesFromCommit(c)
                commits += new Commit(hash: c.name, message: c.fullMessage.replaceAll(RegexUtil.NEW_LINE_REGEX, " "),
                        files: files, author: c.authorIdent.name, date: c.commitTime, isMerge:c.parentCount>1)

            }
        }
    }

    List<Commit> searchByComment(def regex) {
        def result = []
        commits.each { commit ->
            if((commit.message?.toLowerCase() ==~ regex) && !commit.files.empty) result += commit
        }
        result.unique { a, b -> a.hash <=> b.hash }
    }

    String findNewestCommit(List<Commit> commitList){
        if(commitList && !commitList.empty) {
            def hashes = commitList*.hash
            def result = []
            commits.eachWithIndex{ Commit entry, int i ->
                if(entry.hash in hashes){
                    result += [hash:entry.hash, index:i]
                }
            }
            def sortedCommits = result.sort{ it.index }
            sortedCommits.first().hash
        }
        else null
    }

    def createBranchFromCommit(String branch, String commit){
        def git = Git.open(new File(localPath))
        Ref checkout = git.branchCreate()
                .setName(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .setStartPoint(commit)
                .setForce(true)
                .call()
        git.close()
        checkout
    }

    def findObjectId(String sha){
        def git = Git.open(new File(localPath))
        ObjectId objectId = git.repository.resolve(sha)
        git.close()
        objectId
    }

    MergeResult mergeCommitByJgit(String sha){
        ObjectId objectId = findObjectId(sha)
        def git = Git.open(new File(localPath))
        MergeResult mergeResult = git.merge().include(objectId).call()
        git.close()
        mergeResult
    }

    def mergeCommit(String sha){
        def builder = new ProcessBuilder('git','merge', sha)
        builder.directory(new File(localPath))
        def process = builder.start()
        def status = process.waitFor()
        process.inputStream.eachLine { log.info it.toString() }
        status
    }

    def rebaseBranch(String currentBranch, String otherBranch){
        def conflicts = []
        ProcessBuilder builder = new ProcessBuilder("git", "rebase", otherBranch, currentBranch)
        builder.directory(new File(localPath))
        Process process = builder.start()
        int processStatus = process.waitFor()
        def result = process?.inputStream?.readLines()
        process?.inputStream?.close()
        process?.errorStream?.close()

        log.info "Rebase status: ${processStatus}"
        def status = verifyStatus()
        status.each{ log.info it.toString() }
        if(processStatus == 128) conflicts = extractProblematicFilesDuringIntegration(status)
        else if (processStatus!=0) {
            def normalOutput = ""
            result.each{ normalOutput += "${it}\n" }
            def message = "Error while integrating branches ${currentBranch} and ${otherBranch}. " +
                    "Rebase normal output:\n${normalOutput}"
            throw new IntegrationException(message)
        }
        conflicts
    }

    def retrieveIdFromLatestCommitOnBranch(String branch){
        def git = Git.open(new File(localPath))
        git.repository.resolve(branch)
    }

    def verifyStatusDefaultBranch(){
        def objectIdDefaultBranch = retrieveIdFromLatestCommitOnBranch(defaultBranch)
        log.info "Latest commit on branch '$defaultBranch': ${objectIdDefaultBranch.name}"
        log.info "#Commits on branch '$defaultBranch': ${this.verifyCommits(defaultBranch).size()}"
    }

    def stash(){
        ProcessBuilder builder = new ProcessBuilder("git", "stash")
        builder.directory(new File(localPath))
        Process process = builder.start()
        process.inputStream.readLines()
        process.inputStream.close()
    }

    //If the result is null, there is a problem to reproduce merge scenario
    List<String> reproduceMergeScenarioByJgit(String base, String left, String right){
        def conflictingFiles = []
        try {
            this.reset(base)
            this.mergeCommitByJgit(left)
            def mergeResult = this.mergeCommitByJgit(right)
            boolean conflict = (mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING)
            if (conflict) conflictingFiles = mergeResult.conflicts.keySet() as List
            this.revertMerge()
        } catch(Exception ex){
            def message = "Error while merging ${left} and ${right}. Output: ${ex.message}"
            log.warn message
            conflictingFiles = null
        }
        conflictingFiles
    }

    def reproduceMergeScenario(String base, String left, String right){
        this.reset(base)
        def status = this.mergeCommit(left)
        if(status == 0) this.mergeCommit(right)
    }

    //Reproduz commits em branch via rebase; implementação com problema, vide br/ufpe/cin/tas/other/testing/Wpcc_72_97.groovy
    //If the result is null, there is a problem to reproduce merge scenario
    List integrateTasksByRebase1(MergeTask task1, MergeTask task2){
        List<String> conflictingFiles = []
        def branch1 = "branch1_spg"
        def branch2 = "branch2_spg"
        def refOlderBranch
        def refNewerBranch

        //Verify the oldest task
        def taskDate = checkOldestTask(task1, task2)
        MergeTask olderTask = taskDate.olderTask
        MergeTask newerTask = taskDate.newerTask
        log.info "Integrating tasks '${task1.id}' and '${task2.id}'"
        log.info "olderTask: ${olderTask.id}; base: ${olderTask.base}; commits: ${olderTask.commits.size()}"
        log.info "newerTask: ${newerTask.id}; base: ${newerTask.base}; commits: ${newerTask.commits.size()}"

        //Verify common base among tasks
        def gitBase = findBase(olderTask, newerTask)
        log.info "Common base: $gitBase"

        try{
            this.checkoutBranch(defaultBranch)
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            log.info "Creating a new branch from task1's base"
            this.checkoutBranch(branch1, olderTask.base)
            this.verifyStatus()
            this.reset(olderTask.base)
            def commits1 = this.searchRevCommits()
            log.info "#Latest commit on branch '$branch1': ${commits1.first().name} (#commits: ${commits1.size()})"

            log.info "Reproducing the older task's commits in the branch"
            this.reproduceCommits(olderTask, branch1)
            commits1 = this.searchRevCommits()
            log.info "#Latest commit on branch '$branch1': ${commits1.first().name} (#commits: ${commits1.size()})"
            log.info "Latest commit of task1: ${olderTask.newestCommit}"

            this.checkoutBranch(defaultBranch)
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            log.info "Creating a new branch from the newer task's base"
            this.checkoutBranch(branch2, newerTask.base)
            this.reset(newerTask.base)
            def commits2 = this.searchRevCommits()
            log.info "#Latest commit on branch '$branch2': ${commits2.first().name} (#commits: ${commits2.size()})"

            log.info "Reproducing the newer task's commits in the branch"
            this.reproduceCommits(newerTask, branch2)
            commits2 = this.searchRevCommits()
            log.info "#Latest commit on branch '$branch2': ${commits2.first().name} (#commits: ${commits2.size()})"
            log.info "Latest commit of task2: ${newerTask.newestCommit}"

            this.checkoutBranch(defaultBranch)
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            //Integrating tasks
            log.info "Integrating tasks by rebase"
            conflictingFiles = this.rebaseBranch(branch1, branch2)
        } catch(IntegrationException ex){
            log.warn ex.message
            conflictingFiles = null
        } catch(Exception ex){
            log.warn "${ex.class}: ${ex.message}"
            conflictingFiles = null
        } finally {
            this.checkoutBranch(defaultBranch)
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            if(refOlderBranch) {
                this.deleteBranch(branch1)
                log.info "Branch '$branch1' was deleted"
            }

            if(refNewerBranch){
                this.deleteBranch(branch2)
                log.info "Branch '$branch2' was deleted"
            }
        }

        conflictingFiles
    }

    def countCommitsInBranch(String branch){
        ProcessBuilder builder = new ProcessBuilder("git", "rev-list", "--count", branch)
        builder.directory(new File(localPath))
        Process process = builder.start()
        def status = verifyStatus()
        status.each{ log.info it.toString() }
        def counter = process.inputStream.readLines().first().trim() as int
        process?.inputStream?.close()
        counter
    }

    //Reproduz commits em branch via merge; implementação com problema, vide br/ufpe/cin/tas/other/testing/Wpcc_72_97.groovy
    //If the result is null, there is a problem to reproduce merge scenario
    List integrateTasksByRebase2(MergeTask task1, MergeTask task2){
        List<String> conflictingFiles = []
        def branch1 = "branch1_spg"
        def branch2 = "branch2_spg"
        def refOlderBranch
        def refNewerBranch

        //Verify the oldest task
        def taskDate = checkOldestTask(task1, task2)
        MergeTask olderTask = taskDate.olderTask
        MergeTask newerTask = taskDate.newerTask
        log.info "Integrating tasks '${olderTask.id}' and '${newerTask.id}'"
        log.info "olderTask: ${olderTask.id}; base: ${olderTask.base}; commits: ${olderTask.commits.size()}"
        log.info "newerTask: ${newerTask.id}; base: ${newerTask.base}; commits: ${newerTask.commits.size()}"

        //Verify common base among tasks
        def gitBase = findBase(olderTask, newerTask)
        log.info "Common base: $gitBase"

        try{
            this.checkoutBranch(defaultBranch)
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            log.info "Creating a new branch from task1's base"
            this.checkoutBranch(branch1, olderTask.base)
            this.reset(olderTask.base)
            def commits1 = this.searchRevCommits()
            log.info "#Latest commit on branch '$branch1': ${commits1.first().name} (#commits: ${commits1.size()})"
            log.info "Reproducing task1's commits in the branch"
            this.mergeCommitByJgit(olderTask.newestCommit)
            this.verifyStatus()
            commits1 = this.searchRevCommits()
            log.info "#Latest commit on branch '$branch1': ${commits1.first().name} (#commits: ${commits1.size()})"
            log.info "Latest commit of task1: ${olderTask.newestCommit}"

            this.checkoutBranch(defaultBranch)
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            log.info "Creating a new branch from task2's base"
            this.checkoutBranch(branch2, newerTask.base)
            this.reset(newerTask.base)
            def commits2 = this.searchRevCommits()
            log.info "#Latest commit on branch '$branch2': ${commits2.first().name} (#commits: ${commits2.size()})"
            log.info "Reproducing task2's commits in the branch"
            this.mergeCommitByJgit(newerTask.newestCommit)
            this.verifyStatus()
            commits2 = this.searchRevCommits()
            log.info "#Latest commit on branch '$branch2': ${commits2.first().name} (#commits: ${commits2.size()})"
            log.info "Latest commit of task2: ${newerTask.newestCommit}"

            this.checkoutBranch(defaultBranch)
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            //Integrating tasks
            log.info "Integrating tasks by rebase"
            this.checkoutBranch(branch1)
            conflictingFiles = this.rebaseBranch(branch1, branch2)
        } catch(IntegrationException ex){
            log.warn ex.message
            conflictingFiles = null
        } catch(Exception ex){
            log.warn "${ex.class}: ${ex.message}"
            conflictingFiles = null
        } finally {
            this.checkoutBranch(defaultBranch)
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            if(refOlderBranch) {
                this.deleteBranch(branch1)
                log.info "Branch '$branch1' was deleted"
            }

            if(refNewerBranch){
                this.deleteBranch(branch2)
                log.info "Branch '$branch2' was deleted"
            }
        }

        conflictingFiles
    }

    def verifyStatus(){
        ProcessBuilder pb= new ProcessBuilder("git", "status")
        pb.directory(new File(localPath))
        Process process = pb.start()
        process.waitFor()
        def aux = process?.inputStream?.readLines()
        process?.inputStream?.close()
        aux.each{ log.info it.toString() }
        aux
    }

    /* Tutorial: https://chenghlee.com/2013/10/04/git-copying-subset-of-commits/ */
    def reproduceCommits(MergeTask task, String destinationBranch){
        this.checkoutBranch(defaultBranch)
        this.stash()
        log.info "Reproducing ${task.commits.size()} commits"
        def olderHash = task.commits.last().hash
        def latestHash = task.commits.first().hash

        ProcessBuilder pb = new ProcessBuilder("git", "rebase", "-p","--onto", destinationBranch, "${olderHash}^", latestHash)
        pb.directory(new File(localPath))
        Process process = pb.start()
        int exit = process.waitFor()
        def result = process?.inputStream?.readLines()
        process?.inputStream?.close()
        def errorResult = process?.errorStream?.readLines()
        process?.errorStream?.close()

        log.info "Rebase status when reproducing a task's commits: ${exit}"
        if(exit == 0){
            //Reseting the destination branch to the HEAD commit
            this.checkoutBranch(destinationBranch)
        }
        else if(exit==128){
            def conflictingFiles = extractProblematicFilesDuringIntegration(status)
            def conflicts = ""
            conflictingFiles.each{ conflicts += "${it}\n" }
            def message = "Error while reproducing commits of task ${task.id} on branch ${destinationBranch}. " +
                    "Conflicting files (${conflictingFiles.size()}):\n" + conflicts
            throw new IntegrationException(message)
        } else {
            def status = verifyStatus()
            status.each{ log.info it.toString() }

            def normalOutput = ""
            result.each{ normalOutput += "${it}\n" }

            def errorOutput = ""
            errorResult.each{ errorOutput += "${it}\n" }

            def message = "Error while reproducing commits of task ${task.id} on branch ${destinationBranch}. " +
                    "Rebase normal output:\n${normalOutput}\nRebase error output:\n${errorOutput}"
            throw new IntegrationException(message)
        }
    }

    def listCommitsInBranch(String branch){
        verifyCommits(branch)
    }

    /*
    * https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/WalkAllCommits.java
    * */
    Iterable<RevCommit> searchAllRevCommits() {
        def git = Git.open(new File(localPath))
        Iterable<RevCommit> logs = git?.log()?.all()?.call()
        git.close()
        logs
    }

    Iterable<RevCommit> searchRevCommits() {
        def git = Git.open(new File(localPath))
        Iterable<RevCommit> logs = git?.log()?.call()
        git.close()
        logs
    }

    static GitRepository getRepository(String url) {
        if(url==null || url.empty) return null
        def urlOfInterest = url
        if(!urlOfInterest.endsWith(ConstantData.GIT_EXTENSION)) urlOfInterest += ConstantData.GIT_EXTENSION
        def repository = repositories.find { it.url == urlOfInterest }
        if (!repository) {
            repository = new GitRepository(url)
            repositories += repository
        }
        repository
    }

    def getNameFromDefaultBranch(){
        ProcessBuilder builder = new ProcessBuilder("git", "branch")
        builder.directory(new File(localPath))
        Process process = builder.start()
        def lines = process?.inputStream?.readLines()
        process?.inputStream?.close()
        def currentBranch = lines.find{ it.contains("*") }
        if(currentBranch) {
            def index = currentBranch.indexOf("*")
            currentBranch = currentBranch.substring(index+1)?.trim()
        }
        currentBranch
    }

    private static extractProblematicFilesDuringIntegration(List<String> lines){
        def conflicts = []
        def index = lines.findIndexOf { it.contains("Unmerged paths:") }
        if(index>-1){
            def linesOfInterest = lines.subList(index+3, lines.size())
            linesOfInterest.each{
                def i = it.lastIndexOf(":")
                if(i>-1) conflicts += it.substring(i+2, it.size()).trim()
            }
        }
        conflicts = conflicts.collect{
            it.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        }
        conflicts
    }

    private static checkOldestTask(MergeTask task1, MergeTask task2){
        def olderTask
        def newerTask
        if(task1.isBefore(task2)){
            olderTask = task1
            newerTask = task2
        } else {
            olderTask = task2
            newerTask = task1
        }
        [olderTask:olderTask, newerTask:newerTask]
    }

    private verifyCommits(String branch){
        ProcessBuilder builder = new ProcessBuilder("git", "log", "--oneline", branch)
        builder.directory(new File(localPath))
        Process process = builder.start()
        def result = process?.inputStream?.readLines()
        process?.inputStream?.close()
        def hashes = []
        result.each{ hashes.add(it.split(" ").first()) }
        hashes
    }

    private extractDataFromRemoteRepository(String path){
        if(path.endsWith(ConstantData.GIT_EXTENSION)) url = path
        else url = path + ConstantData.GIT_EXTENSION
        this.name = Util.configureGitRepositoryName(url)
        this.localPath = ConstantData.REPOSITORY_FOLDER + name
        if (isCloned()) {
            log.info "Already cloned from " + url + " to " + localPath
        } else cloneRepository()
    }

    private extractDataFromlocalRepository(String path){
        if(path.endsWith(ConstantData.GIT_EXTENSION)) localPath = path - ConstantData.GIT_EXTENSION
        else localPath = path
        def git = Git.open(new File(localPath))
        this.url = git.repository.config.getString("remote", "origin", "url")
        git.close()
        this.name = Util.configureGitRepositoryName(url)
    }

    private boolean isCloned() {
        File dir = new File(localPath)
        File[] files = dir.listFiles()
        if (files && files.length > 0) true
        else false
    }

    private cloneRepository() {
        try {
            def folder = new File(localPath)
            if(!folder.exists()) {
                folder.mkdir()
            }
            String command = "git clone $url $name"
            Process process = Runtime.getRuntime().exec(command, null, new File(ConstantData.REPOSITORY_FOLDER))
            process.waitFor()
        } catch (Exception ex) {
            log.error "Error while cloning repository '${url}' to '${localPath}'."
            ex.stackTrace.each{ log.error it.toString() }
            Util.deleteFolder(localPath)
        }
    }

    private List<DiffEntry> getDiff(RevTree newTree, RevTree oldTree) {
        def git = Git.open(new File(localPath))
        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream())
        df.setRepository(git.repository)
        df.setDiffComparator(RawTextComparator.DEFAULT)
        df.setDetectRenames(true)
        List<DiffEntry> diffs = df.scan(oldTree, newTree)
        List<DiffEntry> result = []
        diffs.each {
            it.oldPath = it.oldPath.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            it.newPath = it.newPath.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            result += it
        }
        git.close()
        result
    }

    private List getAllChangedFilesFromCommit(RevCommit commit) {
        def files = []

        switch (commit.parentCount) {
            case 0: //first commit
                def git = Git.open(new File(localPath))
                TreeWalk tw = new TreeWalk(git.repository)
                tw.reset()
                tw.setRecursive(true)
                tw.addTree(commit.tree)
                while (tw.next()) {
                    files += tw.pathString.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
                }
                //tw.release()
                git.close()
                break
            case 1: //commit with one parent
                def diffs = getDiff(commit.tree, commit.parents.first().tree)
                files = getAllChangedFilesFromDiffs(diffs)
                break
            default: //merge commit (commit with more than one parent)
                commit.parents.each { parent ->
                    def diffs = getDiff(commit.tree, parent.tree)
                    files += getAllChangedFilesFromDiffs(diffs)
                }
        }

        files?.sort()?.unique()
    }

    private static List getAllChangedFilesFromDiffs(List<DiffEntry> diffs) {
        def files = []
        if (!diffs?.empty) {
            diffs.each { entry ->
                if (entry.changeType == DiffEntry.ChangeType.DELETE) files += entry.oldPath
                else {
                    files += entry.newPath
                }
            }
        }
        return files
    }

}
