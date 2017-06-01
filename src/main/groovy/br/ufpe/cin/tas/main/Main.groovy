package br.ufpe.cin.tas.main

import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.search.task.TaskSearchManager

@Slf4j
class Main {

    static void main(String[] args) {
        TaskSearchManager taskSearchManager = new TaskSearchManager()
        taskSearchManager.start()
    }

}
