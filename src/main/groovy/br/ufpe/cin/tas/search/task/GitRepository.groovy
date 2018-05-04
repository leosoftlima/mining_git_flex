package br.ufpe.cin.tas.search.task

import br.ufpe.cin.tas.search.task.merge.MergeScenario
import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
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

    private GitRepository(String path) {
        commits = []
        if (path.startsWith("http")) extractDataFromRemoteRepository(path)
        else extractDataFromlocalRepository(path)
        searchCommits()
        log.info "Project: ${name}"
        log.info "All commits from project: ${commits.size()}"
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

    def extractConflictingFiles(MergeScenario mergeScenario) {
        def conflictingFiles = []
        try{
            this.clean()
            this.reset(mergeScenario.left)
            this.checkout(mergeScenario.left)
            def refNew = this.createBranch(mergeScenario.right)
            MergeResult mergeResult = this.merge(refNew)
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
        else commits
    }

    def searchCommits() {
        if(commits.empty){
            def revCommits = searchAllRevCommits()
            revCommits?.each { c ->
                def files = getAllChangedFilesFromCommit(c)
                commits += new Commit(hash: c.name, message: c.fullMessage.replaceAll(RegexUtil.NEW_LINE_REGEX, " "),
                        files: files, author: c.authorIdent.name, date: c.commitTime)
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

    def createBranch(String commit){
        def git = Git.open(new File(localPath))
        Ref checkout = git.branchCreate()
                .setName("spgstudy")
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .setStartPoint(commit)
                .setForce(true)
                .call()
        git.close()
        checkout
    }

    def merge(Ref refNew){
        def git = Git.open(new File(localPath))
        MergeResult mergeResult = git.merge().include(refNew).call()
        git.close()
        revertMerge()
        mergeResult
    }

    def merge(String left, String right){
        def conflictingFiles = []
        try {
            this.clean()
            this.reset(left)
            this.checkout(left)
            def refNew = this.createBranch(right)
            MergeResult mergeResult = this.merge(refNew)
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

    static GitRepository getRepository(String url) {
        if(url==null || url.empty) return null
        def repository = repositories.find { ((it.url) == url) }
        if (!repository) {
            repository = new GitRepository(url)
            repositories += repository
        }
        repository
    }

    private extractDataFromRemoteRepository(String path){
        if(path.endsWith(ConstantData.GIT_EXTENSION)) url = path
        else url = path + ConstantData.GIT_EXTENSION
        this.name = Util.configureGitRepositoryName(url)
        this.localPath = ConstantData.REPOSITORY_FOLDER + name
        if (isCloned()) {
            log.info "Already cloned from " + url + " to " + localPath
        } else cloneRepository()
        this.lastCommit = searchAllRevCommits()?.last()?.name
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

    private Iterable<RevCommit> searchAllRevCommits() {
        def git = Git.open(new File(localPath))
        Iterable<RevCommit> logs = git?.log()?.all()?.call()
        git.close()
        logs
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
