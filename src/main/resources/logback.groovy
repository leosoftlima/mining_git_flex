import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import util.ConstantData

import static ch.qos.logback.classic.Level.INFO

//Linux: change file name to lower case

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss} [%thread] %-5level %logger{36} %file:%line - %msg%n"
    }
}
appender("FILE", FileAppender) {
    file = ConstantData.LOG_FILE
    append = false
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss} [%thread] %-5level %logger{36} %file:%line - %msg%n"
    }
}
root(INFO, ["FILE", "STDOUT"])