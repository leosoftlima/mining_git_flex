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
            taskSearchManager.findTasks()
        } catch (IOException e) {
            log.info "Problem during projects searching: " + e.getMessage()
        }
    }

}
