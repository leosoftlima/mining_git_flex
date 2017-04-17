package taskSearch

import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.treewalk.TreeWalk
import util.ConstantData
import util.DataProperties
import util.RegexUtil
import util.Util
import java.util.regex.Matcher


@Slf4j
class GitRepository {

    static List<GitRepository> repositories = []

    String url
    String name
    String localPath

    private GitRepository(String path) {
        if (path.startsWith("http")) {
            this.url = path + ConstantData.GIT_EXTENSION
            this.name = Util.configureGitRepositoryName(url)
            this.localPath = DataProperties.REPOSITORY_FOLDER + name
            if (isCloned()) {
                log.info "Already cloned from " + url + " to " + localPath
            } else cloneRepository()
        } else {
            this.localPath = path
            def git = Git.open(new File(localPath))
            this.url = git.repository.config.getString("remote", "origin", "url")
            git.close()
            this.name = Util.configureGitRepositoryName(url)
        }
    }

    private boolean isCloned() {
        File dir = new File(localPath)
        File[] files = dir.listFiles()
        if (files && files.length > 0) true
        else false
    }

    private cloneRepository() {
        try {
            def result = Git.cloneRepository().setURI(url).setDirectory(new File(localPath)).call()
            result.close()
            log.info "Cloned from " + url + " to " + localPath
        } catch (Exception ex) {
            Util.deleteFolder(localPath)
            log.error ex.message
        }
    }

    private Iterable<RevCommit> searchAllRevCommitsBySha(String... hash) {
        def logs
        def git = Git.open(new File(localPath))
        if (hash == null || hash?.length == 0) logs = git?.log()?.call()
        else logs = git?.log()?.call()?.findAll { it.name in hash }?.sort { a, b -> b.commitTime <=> a.commitTime }
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
        def repository = repositories.find { (it.url - ConstantData.GIT_EXTENSION).equals(url) }
        if (!repository) {
            repository = new GitRepository(url)
            repositories += repository
        }
        repository
    }

    List<Commit> searchCommits(String... hash) {
        def revCommits = searchAllRevCommitsBySha(hash)
        def commits = []
        revCommits?.each { c ->
            def files = getAllChangedFilesFromCommit(c)
            commits += new Commit(hash: c.name, message: c.fullMessage.replaceAll(RegexUtil.NEW_LINE_REGEX, " "),
                    files: files, author: c.authorIdent.name, date: c.commitTime)
        }
        commits
    }

    List<Commit> searchByComment(def regex) {
        def commits = searchCommits()
        log.info "Total commits: ${commits.size()}"

        def result = commits.findAll { commit ->
            (commit.message?.toLowerCase() ==~ regex) && !commit.files.empty
        }
        def finalResult = result.unique { a, b -> a.hash <=> b.hash }
        log.info "Total commits by comment: ${finalResult.size()}"

        finalResult.sort { it.date }
    }

}
