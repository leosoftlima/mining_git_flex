package commitSearch

import net.wagstrom.research.github.GithubProperties

import java.util.regex.Matcher


class Task {

    String repositoryIndex
    String repositoryUrl
    String id
    List<Commit> commits
    List<String> productionFiles
    List<String> testFiles

    public Task(String index, String url, String id){
        repositoryIndex = index
        repositoryUrl = url
        this.id = id
        commits = new ArrayList<>()
        productionFiles = new ArrayList<>()
        testFiles = new ArrayList<>()
    }

    public Task(String index, String url, String id, List<Commit> commits){
        this(index, url, id)
        this.commits = commits
        commits*.files?.flatten()?.each{ file ->
            if(isTestCode(file)) testFiles += file
            else productionFiles += file
        }
    }

    private static boolean isTestCode(String path){
        Properties props = GithubProperties.props()
        def testPath = props.getProperty("spgroup.task.interface.path.test").split(",")*.replaceAll(" ", "")

        def regex
        if(testPath.size() > 1){
            regex = ".*("
            testPath.each{ dir ->
                regex += dir+"|"
            }
            regex = regex.substring(0,regex.lastIndexOf("|"))
            regex += ").*"
        }
        else{
            regex = ".*${testPath.get(0)}.*"
        }
        def FILE_SEPARATOR_REGEX = /(\\|\/)/
        regex = regex.replaceAll(FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator)+Matcher.quoteReplacement(File.separator))

        if(path ==~ /$regex/) true
        else false
    }

}
