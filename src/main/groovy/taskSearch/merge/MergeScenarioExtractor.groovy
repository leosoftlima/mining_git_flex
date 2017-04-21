package taskSearch.merge

import groovy.util.logging.Slf4j
import taskSearch.GitRepository
import util.ConstantData
import util.Util

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
        def aux = Util.extractCsvContent(ConstantData.CANDIDATE_REPOSITORIES_FILE)
        if(aux && aux.size()>1) {
            aux?.removeAt(0)
            urls = aux?.collect{ it[1] }
        }
    }

    private exportResult(List<MergeScenario> merges){
        def csv = ConstantData.MERGES_FOLDER+repository.name.replaceAll("/", "_")+ConstantData.MERGE_TASK_SUFIX
        List<String[]> content = []
        String[] header1 = [repository.url]
        String[] header2 = ["MERGE", "LEFT", "RIGHT", "BASE", "LEFT_COMMITS", "RIGHT_COMMITS"]
        content += header2
        merges?.each{
            String[] line = [it.merge, it.left, it.right, it.base, it.leftCommits, it.rightCommits]
            content += line
        }
        Util.createCsv(csv, header1, content)
    }

    private searchMergeCommits(String url){
        def counter = 0
        List<MergeScenario> merges = []

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
                    def parent1  = data[1]
                    def parent2 = data[2]
                    def parentsData = searchLeftRight(parent1, parent2)
                    if(parentsData && parentsData.left.size()>0 && parentsData.right.size()>0){
                        MergeScenario mergeScenario = new MergeScenario(merge: merge, left: parent1, right: parent2,
                                leftCommits: parentsData.left, rightCommits: parentsData.right, base: parentsData.base)
                        merges += mergeScenario
                        log.info mergeScenario.toString()
                    } else counter++
                }
            }
            p1.inputStream.close()
            Collections.reverse(merges)
            exportResult(merges)
            log.info "Selected merges: ${merges.size()}"
            log.info "Fast-fowarding merges: $counter"
        } catch(Exception ex){
            log.error "Error while searching merge commits."
            ex.stackTrace.each{ log.error it.toString() }
        }
    }

    private searchLeftRight(String left, String right){
        ProcessBuilder p1 = new ProcessBuilder("git", "merge-base", left, right)
        p1.directory(new File(repository.localPath))
        Process process1 = p1.start()
        def base
        def aux = process1?.inputStream?.readLines()
        if(!aux || aux.empty) {
            base = ""
            return null
        }
        else base = aux?.first()?.replaceAll("\r","")?.
                replaceAll("\n","")
        process1?.inputStream?.close()

        ProcessBuilder p2 = new ProcessBuilder("git", "rev-list", "${base}..${left}")
        p2.directory(new File(repository.localPath))
        Process process2 = p2.start()
        def leftCommits = process2?.inputStream?.readLines()
        process2?.inputStream?.close()

        ProcessBuilder p3 = new ProcessBuilder("git", "rev-list", "${base}..${right}")
        p3.directory(new File(repository.localPath))
        Process process3 = p3.start()
        def rightCommits = process3?.inputStream?.readLines()
        process3?.inputStream?.close()

        [base: base, left: leftCommits, right: rightCommits]
    }

    def extract(){
        updateUrls()
        urls.each{ searchMergeCommits(it) }
        repository = null
    }

}
