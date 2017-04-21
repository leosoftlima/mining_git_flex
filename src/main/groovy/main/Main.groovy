package main

import groovy.util.logging.Slf4j
import taskSearch.TaskSearchManager

@Slf4j
class Main {

    static void main(String[] args) {
        try {
            TaskSearchManager taskSearchManager = new TaskSearchManager()
            taskSearchManager.searchGithubProjects()
            taskSearchManager.filterGithubProjects()
            taskSearchManager.searchTasks()
        } catch (Exception ex) {
            log.info "Problem during projects searching."
            log.info ex.message
            ex.stackTrace.each{ log.error it.toString() }
        }
    }

}
