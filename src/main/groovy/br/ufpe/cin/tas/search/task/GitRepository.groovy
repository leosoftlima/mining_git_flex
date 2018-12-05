package br.ufpe.cin.tas.search.task

import br.ufpe.cin.tas.search.task.merge.MergeScenario
import br.ufpe.cin.tas.search.task.merge.MergeTask
import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.CherryPickResult
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
    List<RevCommit> revCommits

    private GitRepository(String path) {
        commits = []
        branch = "spgstudy"
        if (path.startsWith("http")) extractDataFromRemoteRepository(path)
        else extractDataFromlocalRepository(path)
        searchCommits()
        log.info "Project: ${name}"
        log.info "All commits from project: ${commits.size()}"
        lastCommit = commits.first().hash
        revCommits = searchAllRevCommits().asList()
        log.info "All revcommits from project: ${revCommits.size()}"
    }

    def checkoutBranch(String branch){
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "checkout", "-B", branch)
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def checkout(String sha) {
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "checkout", sha)
        processBuilderMerges.directory(new File(localPath))
        Process p1 = processBuilderMerges.start()
        p1.waitFor()
    }

    def checkout() {
        ProcessBuilder processBuilderMerges = new ProcessBuilder("git", "checkout", lastCommit)
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
        def result = p1?.inputStream?.readLines()
        p1?.inputStream?.close()
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

    def getCommitSetBetweenFirstParentOption(String base, String other){
        ProcessBuilder builder = new ProcessBuilder("git", "rev-list", "${base}..${other}", "--first-parent")
        builder.directory(new File(localPath))
        Process process = builder.start()
        def commitSet = process?.inputStream?.readLines()
        process?.inputStream?.close()
        commitSet
    }

    def deleteBranch(){
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

    List integrateTasksByCherryPick(MergeTask task1, MergeTask task2){
        List<String> conflictingFiles = []
        def olderTask
        def newerTask

        try{
            //Verify the oldest task
            if(task1.isBefore(task2)){
                olderTask = task1
                newerTask = task2
            } else {
                olderTask = task2
                newerTask = task1
            }
            log.info "Integrating tasks '${task1.id}' and '${task2.id}'"
            log.info "olderTask: ${olderTask.id}"
            log.info "newerTask: ${newerTask.id}"
            log.info "base: ${olderTask.base}"

            //Update the master with the older task's base
            this.checkout("master")
            this.reset(olderTask.base)
            this.checkout(olderTask.base)
            this.clean()
            def result = listCommitsInMaster()
            log.info "master was updated with base"
            log.info "master commits: ${result?.size()}"

            //Create a new branch from the base
            def spgBranch = this.createBranchFromCommit(olderTask.base)
            log.info "'$branch' was created"
            result = listCommitsInBranch(branch)
            log.info "branch '$branch' commits: ${result?.size()}"

            //Reproduce the older task's commits in the master
            this.checkout("master")
            def conflicts1 = this.cherryPick(olderTask, "1")
            result = listCommitsInMaster()
            log.info "master was updated with the older task's commits"
            log.info "master commits: ${result?.size()}"

            //Reproduce the newer task's commits in the branch
            this.checkout(branch)
            def conflicts2 = this.cherryPick(newerTask, "1")
            result = listCommitsInBranch(branch)
            log.info "branch '$branch' was updated with the newest task's commits"
            log.info "branch '$branch' commits: ${result?.size()}"

            //Merge the branch into the master
            conflictingFiles = conflicts1 + conflicts2
            if(conflictingFiles.empty){
                log.info "we will try to merge branch $branch into master"
                this.checkout("master")
                MergeResult mergeResult = this.mergeBranch(spgBranch)
                log.info "branch '$branch' was merged into master"
                boolean conflict = (mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING)
                if (conflict) {
                    conflictingFiles = mergeResult.conflicts.keySet() as List
                }
            } else {
                log.warn "we cannot merge branch '$branch' into master because there is some conflicts"
            }
        } catch(Exception ex){
            log.warn "Exception while integrating tasks: ${ex.message}"
            ex.stackTrace.each{ log.warn it.toString() }
            conflictingFiles = null
        } finally {
            this.checkout("master")
            this.deleteBranch()
            log.info "branch '$branch' was deleted"
            this.clean()
            this.resetToLastCommit()
        }
        conflictingFiles = conflictingFiles.collect{
            it.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        }
        conflictingFiles
    }

    List integrateTasksByRebase(MergeTask task1, MergeTask task2){
        List<String> conflictingFiles = []
        def olderTask
        def newerTask

        try{
            //Verify the oldest task
            if(task1.isBefore(task2)){
                olderTask = task1
                newerTask = task2
            } else {
                olderTask = task2
                newerTask = task1
            }
            log.info "Integrating tasks '${task1.id}' and '${task2.id}'"
            log.info "olderTask: ${olderTask.id}"
            log.info "newerTask: ${newerTask.id}"
            log.info "base: ${olderTask.base}"

            //Update the master with the older task's base
            this.checkout("master")
            this.reset(olderTask.base)
            this.checkout(olderTask.base)
            this.clean()
            def result = listCommitsInMaster()
            log.info "master was updated with base"
            log.info "master commits: ${result?.size()}"

            //Create a new branch from the base
            this.createBranchFromCommit(olderTask.base)
            log.info "'$branch' was created"
            result = listCommitsInBranch(branch)
            log.info "branch '$branch' commits: ${result?.size()}"

            //Reproduce the older task's commits in the branch
            def conflicts1 = this.rebase(olderTask, branch)
            this.checkoutBranch(branch)
            result = listCommitsInBranch(branch)
            log.info "branch '$branch' was updated with the older task's commits"
            log.info "branch '$branch' commits: ${result?.size()}"

            //Reproduce the newer task's commits in the branch
            def conflicts2 = this.rebase(newerTask, branch)
            this.checkoutBranch(branch)
            result = listCommitsInBranch(branch)
            log.info "branch '$branch' was updated with the newest task's commits"
            log.info "branch '$branch' commits: ${result?.size()}"

            conflictingFiles = conflicts1 + conflicts2
        } catch(Exception ex){
            log.warn "Exception while integrating tasks: ${ex.message}"
            ex.stackTrace.each{ log.warn it.toString() }
            conflictingFiles = null
        } finally {
            this.checkout("master")
            this.deleteBranch()
            log.info "branch '$branch' was deleted"
            this.clean()
            this.resetToLastCommit()
        }
        conflictingFiles = conflictingFiles.collect{
            it.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        }
        conflictingFiles
    }

    def cherryPick(MergeTask task, String parent){
        log.info "Cherry-pick: ${task.commits.size()} commits"
        def conflictingFiles = []

        def commitsSet = task.commits.reverse()
        for(def i=0; i<commitsSet.size(); i++){
            def commit = commitsSet.get(i)
            def git = Git.open(new File(localPath))
            RevCommit revCommit = revCommits.find{ it.name == commit.hash }
            CherryPickResult result = null
            if(revCommit){
                if(commit.isMerge){
                    result = git.cherryPick()
                            .include(revCommit.id)
                            .setMainlineParentNumber(parent as int)
                            .call()
                } else {
                    result = git.cherryPick()
                            .include(revCommit.id)
                            .call()
                }
            } else {
                log.warn "Revcommit not found: ${commit.hash}"
            }
            git.close()

            log.info "Cherry-pick status: ${result?.status}"
            if(result?.status == CherryPickResult.CherryPickStatus.CONFLICTING){
                def status = verifyStatus()
                status.each{ log.info it.toString() }
                conflictingFiles += extractProblematicFilesDuringCherryPick(status)
            } else if(result?.status == CherryPickResult.CherryPickStatus.FAILED){
                def status = verifyStatus()
                status.each{ log.info it.toString() }
                conflictingFiles += result?.failingPaths?.keySet() as List
            }
            if(result?.status != CherryPickResult.CherryPickStatus.OK) {
                this.reset()
                def status = verifyStatus()
                status.each{ log.info it.toString() }
                log.warn "Cherry-pick was aborted"
                break
            }
        }
        conflictingFiles
    }

    def verifyStatus(){
        ProcessBuilder pb= new ProcessBuilder("git", "status")
        pb.directory(new File(localPath))
        Process process = pb.start()
        int exit = process.waitFor()
        def aux = process?.inputStream?.readLines()
        process?.inputStream?.close()
        aux
    }

    def rebase(MergeTask task, String branch){
        def conflictingFiles = []
        log.info "rebasing ${task.commits.size()} commits"
        def newRoot = branch
        def oldRoot = task.commits.last().hash //from
        def oldTip = task.commits.first().hash //to
        ProcessBuilder pb = new ProcessBuilder("git", "rebase", "--onto", newRoot, "${oldRoot}^", oldTip)
        pb.directory(new File(localPath))
        Process process = pb.start()
        //StreamGobbler errorGobbler = new StreamGobbler(process.errorStream, "ERROR")
        //StreamGobbler outputGobbler = new StreamGobbler(process.inputStream, "OUTPUT")
        //outputGobbler.start()
        //errorGobbler.start()
        int exit = process.waitFor()
        //errorGobbler.join()
        //outputGobbler.join()
        def status = verifyStatus()
        log.info "Rebase status: ${exit}"
        status.each{ log.info it.toString() }

        if (status.find{ it.contains("Unmerged paths:")}) {
            conflictingFiles += extractProblematicFilesDuringCherryPick(status)
            this.reset()
        }
        conflictingFiles
    }

    def listCommitsInMaster(){
        verifyCommits("master")
    }

    def listCommitsInBranch(String branch){
        verifyCommits(branch)
    }

    Iterable<RevCommit> searchAllRevCommits() {
        def git = Git.open(new File(localPath))
        Iterable<RevCommit> logs = git?.log()?.all()?.call()
        git.close()
        logs
    }

    static GitRepository getRepository(String url) {
        if(url==null || url.empty) return null
        def repository = repositories.find { ((it.url) == url) }
        if (!repository) {
            repository = new GitRepository(url)
            repositories += repository
        }
        repository
    }

    private static extractProblematicFilesDuringCherryPick(List<String> lines){
        def conflicts = []
        def index = lines.findIndexOf { it.contains("Unmerged paths:") }
        if(index>-1){
            def linesOfInterest = lines.subList(index+3, lines.size())
            linesOfInterest.each{
                def i = it.indexOf(":")
                if(i>-1) conflicts += it.substring(i+2, it.size()).trim()
            }
        }
        conflicts
    }

    /*private static class StreamGobbler extends Thread {

        private final InputStream is;
        private final String type;

        private StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        @Override
        void run() {
            BufferedReader br = null
            try {
                br = new BufferedReader(new InputStreamReader(is))
                String line
                while ((line = br.readLine()) != null) {
                   log.info "${type}> ${line}"
                }
            } catch (IOException ioe) {
                ioe.printStackTrace()
            } finally {
                if(br) br.close()
            }
        }
    }*/

    private verifyCommits(String branch){
        ProcessBuilder pb = new ProcessBuilder("git", "log", branch, "--oneline")
        pb.directory(new File(localPath))
        Process p1 = pb.start()
        def result = p1?.inputStream?.readLines()
        p1?.inputStream?.close()
        result
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
