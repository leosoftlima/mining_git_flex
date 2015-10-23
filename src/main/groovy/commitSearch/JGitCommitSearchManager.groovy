package commitSearch

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import repositorySearch.Repository

import java.util.regex.Matcher


/***
 * Provides searching mechanism based on JGit for GitHub repositories.
 */
class JGitCommitSearchManager {

    Repository repository
    ObjectReader reader
    static final FILE_SEPARATOR_REGEX = /(\\|\/)/
    static final NEW_LINE_REGEX = /\r\n|\n/

    public JGitCommitSearchManager(){
        def gitPath = "${System.getProperty("user.home")}${File.separator}Documents${File.separator}GitHub" +
                "${File.separator}MiningGit${File.separator}tmp${File.separator}.git"
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        repository = builder.setGitDir(new File(gitPath)).setMustExist(true).build()
        reader = repository.newObjectReader()
    }

    public JGitCommitSearchManager(String gitPath){
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        repository = builder.setGitDir(new File(gitPath)).setMustExist(true).build()
        reader = repository.newObjectReader()
    }

    private List<Commit> extractCommitsFromLogs(Iterable<RevCommit> logs){
        def commits = []
        logs.each{ c ->
            def files = getAllChangedFilesFromCommit(c)
            commits += new Commit(hash:c.name, message:c.fullMessage.replaceAll(NEW_LINE_REGEX," "), files:files,
                    author:c.authorIdent.name, date:c.commitTime)
        }
        return commits
    }

    private List<DiffEntry> getDiff(String filename, RevTree newTree, RevTree oldTree){
        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream())
        df.setRepository(repository)
        df.setDiffComparator(RawTextComparator.DEFAULT)
        df.setDetectRenames(true)
        if(filename!=null && !filename.isEmpty()) df.setPathFilter(PathFilter.create(filename))
        List<DiffEntry> diffs = df.scan(oldTree, newTree)
        List<DiffEntry> result = []
        diffs.each{
            it.oldPath = it.oldPath.replaceAll(FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            it.newPath = it.newPath.replaceAll(FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            result += it
        }
        return result
    }

    private static List getAllChangedFilesFromDiffs(List<DiffEntry> diffs) {
        def files = []
        if (!diffs?.empty) {
            diffs.each{ entry ->
                if(entry.changeType==DiffEntry.ChangeType.DELETE) files += entry.oldPath
                else {
                    files += entry.newPath
                }
            }
        }
        return files
    }

    private List getAllChangedFilesFromCommit(RevCommit commit){
        def files = []
        if(commit.parentCount>0) {
            def diffs = getDiff(null, commit.tree, commit.parents.first().tree)
            files = getAllChangedFilesFromDiffs(diffs)
        }
        else{
            TreeWalk tw = new TreeWalk(repository)
            tw.reset()
            tw.setRecursive(true)
            tw.addTree(commit.tree)
            while(tw.next()){
                files += tw.pathString.replaceAll(FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            }
            tw.release()
        }
        return files
    }

    /***
     * Retrieves all commits from a git repository.
     * @return all commits
     */
    public List<Commit> searchAllCommits(){
        Git git = new Git(repository)
        Iterable<RevCommit> logs = git.log().call()
        def commits = extractCommitsFromLogs(logs)
        return commits.sort{ it.date }
    }

    /***
     * Retrieves all commits from a git repository filtered by keywords founded in the commit message.
     * @param words keywords for the searching
     * @return commits that satisfies the searching criteria
     */
    public List<Commit> searchByComment(List<String> words){
        def commits = searchAllCommits()
        println "Total commits: ${commits.size()}"

        def result = commits.findAll{ commit ->
            words?.any{commit.message.toLowerCase().contains(it)} && !commit.files.empty
        }
        def finalResult = result.unique{ a,b -> a.hash <=> b.hash }
        println "Total commits by comment: ${finalResult.size()}"

        return finalResult.sort{ it.date }
    }

    /***
     * Retrieves all commits from a git repository which the commit message satisfies a regular expression.
     * @param regex regular expression that defines the expected format of the commit message
     * @return commits that satisfies the searching criteria
     */
    public List<Commit> searchByComment(String regex){
        def commits = searchAllCommits()
        println "Total commits: ${commits.size()}"

        def result = commits.findAll{ commit ->
            (commit.message.toLowerCase() ==~ regex) && !commit.files.empty
        }
        def finalResult = result.unique{ a,b -> a.hash <=> b.hash }
        println "Total commits by comment: ${finalResult.size()}"

        return finalResult.sort{ it.date }
    }

}
