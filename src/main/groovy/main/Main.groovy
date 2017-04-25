package main

import groovy.util.logging.Slf4j
import taskSearch.TaskSearchManager

@Slf4j
class Main {

    static void main(String[] args) {
        TaskSearchManager taskSearchManager = new TaskSearchManager()
        taskSearchManager.start()
    }

}
