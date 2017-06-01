package br.ufpe.cin.tas.search.task

import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
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

    private GitRepository(String path) {
        if (path.startsWith("http")) {
            if(path.endsWith(ConstantData.GIT_EXTENSION)) url = path
            else url = path + ConstantData.GIT_EXTENSION
            this.name = Util.configureGitRepositoryName(url)
            this.localPath = ConstantData.REPOSITORY_FOLDER + name
            if (isCloned()) {
                log.info "Already cloned from " + url + " to " + localPath
            } else cloneRepository()
        } else {
            if(path.endsWith(ConstantData.GIT_EXTENSION)) localPath = path - ConstantData.GIT_EXTENSION
            else localPath = path
            def git = Git.open(new File(localPath))
            this.url = git.repository.config.getString("remote", "origin", "url")
            git.close()
            this.name = Util.configureGitRepositoryName(url)
        }
        commits = searchCommits()
        log.info "All commits from project: ${commits.size()}"
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

    private Iterable<RevCommit> searchAllRevCommitsBySha() {
        def git = Git.open(new File(localPath))
        def logs = git?.log()?.call()
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

    static GitRepository getRepository(String url) {
        def repository = repositories.find { (it.url).equals(url) }
        if (!repository) {
            repository = new GitRepository(url)
            repositories += repository
        }
        repository
    }

    def searchCommits(List hashes){
        if(hashes && !hashes.empty) commits.findAll{ it.hash in hashes }
        else commits
    }

    List<Commit> searchCommits() {
        def revCommits = searchAllRevCommitsBySha()
        def commits = []
        revCommits?.each { c ->
            def files = getAllChangedFilesFromCommit(c)
            commits += new Commit(hash: c.name, message: c.fullMessage.replaceAll(RegexUtil.NEW_LINE_REGEX, " "),
                    files: files, author: c.authorIdent.name, date: c.commitTime)
        }
        commits
    }

    List<Commit> searchByComment(def regex) {
        def result = commits.findAll { commit ->
            (commit.message?.toLowerCase() ==~ regex) && !commit.files.empty
        }
        def finalResult = result.unique { a, b -> a.hash <=> b.hash }
        finalResult.sort { it.date }
    }

}
