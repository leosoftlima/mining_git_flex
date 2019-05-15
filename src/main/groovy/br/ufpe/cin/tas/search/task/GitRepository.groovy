package br.ufpe.cin.tas.search.task

import br.ufpe.cin.tas.search.task.merge.MergeScenario
import br.ufpe.cin.tas.search.task.merge.MergeTask
import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
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
    String branch
    String defaultBranch

    private GitRepository(String path) {
        commits = []
        branch = "spgstudy"
        if (path.startsWith("http")) extractDataFromRemoteRepository(path)
        else extractDataFromlocalRepository(path)
        searchCommits()
        lastCommit = commits.first().hash
        log.info "Project: ${name}"
        log.info "All commits from project: ${commits.size()}"
        defaultBranch = nameFromDefaultBranch
        log.info "Default branch: ${defaultBranch}"
    }

    def checkoutBranch(String branch){
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "checkout", "-B", branch)
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def checkout(String sha) {
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "checkout", "-f", sha)
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def checkout() {
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "checkout", "-f", lastCommit)
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def clean() {
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "clean", "-f", "-d", "-x")
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def reset(String sha){
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "reset", "--hard", sha)
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def reset(){
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "reset", "--hard")
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def resetToLastCommit(){
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "reset", "--hard", lastCommit)
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def revertMerge(){
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "merge", "--abort")
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def searchMergeCommits(){
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "log", "--merges")
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        def lines = p1?.inputStream?.readLines()
        p1?.inputStream?.close()
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
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "merge-base", left, right)
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
        def aux = p1?.inputStream?.readLines()
        p1?.inputStream?.close()
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

    def deleteBranch(){
        deleteBranch(branch)
    }

    def deleteBranch(String branch){
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "branch", "-D", branch)
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def extractConflictingFiles(MergeScenario mergeScenario) {
        def conflictingFiles = []
        try{
            this.clean()
            this.reset(mergeScenario.left)
            this.checkout(mergeScenario.left)
            def refNew = this.createBranchFromCommit(mergeScenario.right)
            MergeResult mergeResult = this.mergeBranch(refNew)
            boolean conflict = (mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING)
            if (conflict) {
                conflictingFiles = mergeResult.conflicts.keySet() as List
            }
        } catch(ignored){
            this.clean()
            this.resetToLastCommit()
            this.checkout()
            conflictingFiles = null
        }
        conflictingFiles
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

    def createBranch(){
        def git = Git.open(new File(localPath))
        Ref checkout = git.branchCreate()
                .setName(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .setForce(true)
                .call()
        git.close()
        checkout
    }

    def createBranchFromCommit(String commit){
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

    def mergeBranch(Ref refNew){
        def git = Git.open(new File(localPath))
        MergeResult mergeResult = git.merge().include(refNew).call()
        git.close()
        revertMerge()
        mergeResult
    }

    def merge(String commit1, String commit2){
        def conflictingFiles = []
        try {
            this.reset(commit1)
            this.clean()
            def refNew = this.createBranchFromCommit(commit2)
            this.checkout("master")
            MergeResult mergeResult = this.mergeBranch(refNew)
            this.deleteBranch()
            boolean conflict = (mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING)
            if (conflict) {
                conflictingFiles = mergeResult.conflicts.keySet() as List
            }
        } catch(ignored){
            this.clean()
            this.resetToLastCommit()
            this.checkout()
            conflictingFiles = null
        }
        conflictingFiles
    }

    def rebaseBranch(String currentBranch, String otherBranch){
        def lastConflictingFiles = []
        ProcessBuilder pb = new ProcessBuilder("git", "rebase", otherBranch, currentBranch)
        pb.directory(new File(localPath))
        Process process = pb.start()
        int exit = process.waitFor()
        def result = process?.inputStream?.readLines()
        process?.inputStream?.close()
        log.info "Result of reproducing commits: ${result.size()}"
        result.each{ log.info it.toString() }

        def status = verifyStatus()
        log.info "Rebase status: ${exit}"
        status.each{ log.info it.toString() }
        if (status.find{ it.contains("Unmerged paths:")}) {
            lastConflictingFiles = extractProblematicFilesDuringIntegration(status)
            lastConflictingFiles.each{ cf ->
                new File(localPath + File.separator+ (cf as String)).readLines().each{ println it }
            }
        }
        lastConflictingFiles
    }

    def retrieveIdFromLatestCommitOnBranch(String branch){
        def git = Git.open(new File(localPath))
        git.repository.resolve(branch)
    }

    def verifyStatusDefaultBranch(){
        def objectIdDefaultBranch = retrieveIdFromLatestCommitOnBranch(defaultBranch)
        log.info "Latest commit on branch '$defaultBranch': ${objectIdDefaultBranch.name}"
        log.info "#Commits on branch '$defaultBranch': ${this.listCommitsInBranch(defaultBranch).size()}"
    }

    def stash(){
        ProcessBuilder pb = new ProcessBuilder("git", "stash")
        pb.directory(new File(localPath))
        Process process = pb.start()
        def status = verifyStatus()
        status.each{ log.info it.toString() }
    }

    //If the result is null, there is a problem to reproduce merge scenario
    List integrateTasks(MergeTask task1, MergeTask task2){
        List<String> conflictingFiles = []
        def olderBranch = "olderBranch_spg"
        def newerBranch = "newerBranch_spg"
        def refOlderBranch
        def refNewerBranch

        //Verify the oldest task
        def taskDate = checkOldestTask(task1, task2)
        MergeTask olderTask = taskDate.olderTask
        MergeTask newerTask = taskDate.newerTask
        log.info "Integrating tasks '${task1.id}' and '${task2.id}'"
        log.info "olderTask: ${olderTask.id}; base: ${olderTask.base}; commits: ${olderTask.commits.size()}"
        log.info "newerTask: ${newerTask.id}; base: ${newerTask.base}; commits: ${newerTask.commits.size()}"

        //Verigy common base among tasks
        def gitBase = findBase(olderTask, newerTask)
        log.info "Common base: $gitBase"

        try{
            this.verifyStatusDefaultBranch()

            log.info "Creating a new branch from the older task's base"
            refOlderBranch = this.createBranchFromCommit(olderBranch, olderTask.base)
            log.info "'$olderBranch' was created"
            def objectId1 = this.retrieveIdFromLatestCommitOnBranch(olderBranch)
            log.info "Latest commit on branch '$olderBranch': ${objectId1.name}"
            log.info "#Commits on branch '$olderBranch': ${this.listCommitsInBranch(olderBranch).size()}"

            log.info "Reproducing the older task's commits in the branch"
            this.reproduceCommits(olderTask, olderBranch)
            objectId1 = this.retrieveIdFromLatestCommitOnBranch(olderBranch)
            log.info "Latest commit on branch '$olderBranch': ${objectId1.name}"
            log.info "#Commits on branch '$olderBranch': ${this.listCommitsInBranch(olderBranch).size()}"

            this.checkoutBranch(defaultBranch)
            this.verifyStatusDefaultBranch()
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            log.info "Creating a new branch from the newer task's base"
            refNewerBranch = this.createBranchFromCommit(newerBranch, newerTask.base)
            log.info "'$newerBranch' was created"
            def objectId2 = retrieveIdFromLatestCommitOnBranch(newerBranch)
            log.info "Latest commit on branch '$newerBranch': ${objectId2.name}"
            log.info "#Commits on branch '$newerBranch': ${this.listCommitsInBranch(newerBranch).size()}"

            log.info "Reproducing the newer task's commits in the branch"
            this.reproduceCommits(newerTask, newerBranch)
            objectId2 = retrieveIdFromLatestCommitOnBranch(newerBranch)
            log.info "Latest commit on branch '$newerBranch': ${objectId2.name}"
            def commitsOnNewerBranch = this.listCommitsInBranch(newerBranch)
            log.info "#Commits on branch '$newerBranch': ${commitsOnNewerBranch.size()}"
            olderTask.commits*.hash.each{
                if(commitsOnNewerBranch.contains(it)){
                    log.info "commit '${it}' from the older branch is part of the newer branch"
                }
            }

            this.checkoutBranch(defaultBranch)
            this.verifyStatusDefaultBranch()
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            //Integrating tasks
            log.info "Integrating tasks by rebase"
            this.rebaseBranch(olderBranch, newerBranch)
            objectId1 = this.retrieveIdFromLatestCommitOnBranch(olderBranch)
            log.info "Latest commit on branch '$olderBranch': ${objectId1.name}"
            log.info "#Commits on branch '$olderBranch': ${this.listCommitsInBranch(olderBranch).size()}"
            objectId2 = retrieveIdFromLatestCommitOnBranch(newerBranch)
            log.info "Latest commit on branch '$newerBranch': ${objectId2.name}"
            log.info "#Commits on branch '$newerBranch': ${this.listCommitsInBranch(newerBranch).size()}"

        } catch(Exception ex){
            log.warn "Exception while integrating tasks: ${ex.message}"
            ex.stackTrace.each{ log.warn it.toString() }
            conflictingFiles = null
        } finally {
            this.checkoutBranch(defaultBranch)
            this.resetToLastCommit()
            this.verifyStatusDefaultBranch()

            if(refOlderBranch) {
                this.deleteBranch(olderBranch)
                log.info "Branch '$olderBranch' was deleted"
            }

            if(refNewerBranch){
                this.deleteBranch(newerBranch)
                log.info "Branch '$newerBranch' was deleted"
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
        aux
    }

    private extractConflictingFilesFromRebaseOutput(def exit){
        def conflictingFiles = []
        def status = verifyStatus()
        log.info "Rebase status: ${exit}"
        status.each{ log.info it.toString() }

        if (status.find{ it.contains("Unmerged paths:")}) {
            conflictingFiles += extractProblematicFilesDuringIntegration(status)
            /*conflictingFiles.each{ cf ->
                new File(localPath + File.separator + (cf as String)).readLines().each{ println it }
            }*/
        }
        conflictingFiles
    }

    /* Tutorial: https://chenghlee.com/2013/10/04/git-copying-subset-of-commits/ */
    def reproduceCommits(MergeTask task, String destinationBranch){
        this.checkoutBranch(defaultBranch)
        this.stash()
        def conflictingFiles = []
        log.info "Reproducing ${task.commits.size()} commits"
        def olderHash = task.commits.last().hash
        def latestHash = task.commits.first().hash

        ProcessBuilder pb = new ProcessBuilder("git", "rebase", "-p","--onto", destinationBranch, "${olderHash}^", latestHash)
        pb.directory(new File(localPath))
        Process process = pb.start()
        def exit = process.waitFor()
        def result = process?.inputStream?.readLines()
        process?.inputStream?.close()
        def errorResult = process?.errorStream?.readLines()
        process?.errorStream?.close()

        log.info "Result of reproducing commits: ${result.size()}"
        result.each{ log.info it.toString() }

        log.info "Error result of reproducing commits: ${errorResult.size()}"
        errorResult.each{ log.info it.toString() }

        conflictingFiles = extractConflictingFilesFromRebaseOutput(exit)

        //Reseting the destination branch to the HEAD commit
        if(conflictingFiles.empty) this.checkoutBranch(destinationBranch)
        else {
            log.warn "Error while reproducing commits on branch '${destinationBranch}'"
            log.info "Conflicts: ${conflictingFiles.size()}"
            conflictingFiles.each{ log.info it.toString() }
            throw new Exception("Error while reproducing commits on branch '${destinationBranch}'")
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
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "branch")
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        def lines = p1?.inputStream?.readLines()
        p1?.inputStream?.close()
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
                def i = it.indexOf(":")
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
        ProcessBuilder pb = new ProcessBuilder("git", "log", "--oneline", branch)
        pb.directory(new File(localPath))
        Process p1 = pb.start()
        def result = p1?.inputStream?.readLines()
        p1?.inputStream?.close()
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
            Process p = Runtime.getRuntime().exec(command, null, new File(ConstantData.REPOSITORY_FOLDER))
            p.waitFor()
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
