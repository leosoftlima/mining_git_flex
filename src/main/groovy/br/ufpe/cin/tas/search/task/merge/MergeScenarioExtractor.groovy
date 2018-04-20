package br.ufpe.cin.tas.search.task.merge

import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.search.task.GitRepository
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.CsvUtil
import br.ufpe.cin.tas.util.RegexUtil
import br.ufpe.cin.tas.util.Util

@Slf4j
class MergeScenarioExtractor {
    GitRepository repository
    ProcessBuilder processBuilderMerges
    List<String> urls

    MergeScenarioExtractor() {
        processBuilderMerges = new ProcessBuilder("git", "log", "--merges")
        urls = []
    }

    private updateUrls(){
        def aux = CsvUtil.read(ConstantData.CANDIDATE_REPOSITORIES_FILE)
        if(aux && aux.size()>1) {
            aux?.removeAt(0)
            urls = aux?.collect{ it[0] }
        } else urls = []
    }

    private exportResult(List<MergeScenario> merges){
        def csv = ConstantData.MERGES_FOLDER+repository.name.replaceAll("/", "_")+ConstantData.MERGE_TASK_SUFIX
        List<String[]> content = []
        String[] header1 = [repository.url]
        content += header1
        String[] header2 = ["MERGE", "LEFT", "RIGHT", "BASE", "LEFT_COMMITS", "RIGHT_COMMITS"]
        content += header2
        merges?.each{
            String[] line = [it.merge, it.left, it.right, it.base, it.leftCommits, it.rightCommits]
            content += line
        }
        CsvUtil.write(csv, content)
    }

    private searchMergeCommits(String url){
        def counter = 0
        List<MergeScenario> merges = []

        if(url==null || url.empty) {
            log.warn "It is not possible to extract merge commits for invalid project: url is empty!"
        }
        else{
            try{
                log.info "Extracting merge commit from project '$url'"
                repository = GitRepository.getRepository(url)
                processBuilderMerges.directory(new File(repository.localPath))
                Process p1 = processBuilderMerges.start()
                List<String> lines = p1.inputStream.readLines()
                lines?.eachWithIndex { line, index ->
                    if(line.startsWith('commit')){
                        def merge = line.split(' ')[1]
                        def nextLine = lines.get(index+1)
                        String[] data = nextLine.split(' ')
                        def left  = data[1]
                        def right = data[2]
                        MergeScenario mergeScenario = configureMergeScenario(left, right)
                        if(mergeScenario){
                            mergeScenario.merge = merge
                            merges += mergeScenario
                            log.info mergeScenario.toString()
                        } else counter++
                    }
                }
                p1.inputStream.close()
                exportResult(merges)
                log.info "All merge commits: ${merges.size()+counter}"
                log.info "Fast-fowarding merges: $counter"
                log.info "Selected merges: ${merges.size()}"
            } catch(Exception ex){
                log.error "Error while searching merge commits."
                ex.stackTrace.each{ log.error it.toString() }
            }
        }
    }

    private findBase(String left, String right){
        ProcessBuilder builder = new ProcessBuilder("git", "merge-base", left, right)
        builder.directory(new File(repository.localPath))
        Process process = builder.start()
        def aux = process?.inputStream?.readLines()
        process?.inputStream?.close()
        if(!aux || aux.empty) return null
        else aux?.first()?.replaceAll(RegexUtil.NEW_LINE_REGEX,"")
    }

    private getCommitSetBetween(String base, String other){
        ProcessBuilder builder = new ProcessBuilder("git", "rev-list", "${base}..${other}")
        builder.directory(new File(repository.localPath))
        Process process = builder.start()
        def commitSet = process?.inputStream?.readLines()?.reverse()
        process?.inputStream?.close()
        commitSet
    }

    private configureMergeScenario(String left, String right){
        def base = findBase(left, right)
        if(!base || base.empty) { return null }
        def leftCommits = getCommitSetBetween(base, left)
        def rightCommits = getCommitSetBetween(base, right)
        if(leftCommits.empty || rightCommits.empty) return null
        def leftHash = leftCommits.last()
        def rightHash = rightCommits.last()
        new MergeScenario(left: leftHash, right: rightHash, leftCommits: leftCommits, rightCommits: rightCommits, base: base)
    }

    def generateMergeFiles(){
        updateUrls()
        urls?.each{ searchMergeCommits(it) }
        repository = null
    }

    def getMergeFiles(){
        generateMergeFiles()
        def mergeFiles = Util.findFilesFromFolder(ConstantData.MERGES_FOLDER)?.findAll{
            it.endsWith(ConstantData.MERGE_TASK_SUFIX)
        }
        mergeFiles
    }

}
