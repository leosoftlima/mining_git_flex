import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import static ch.qos.logback.classic.Level.INFO

//Linux: change file name to lower case

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss} [%thread] %-5level %logger{36} %file:%line - %msg%n"
    }
}
appender("FILE", FileAppender) {
    file = "output${File.separator}execution.log"
    append = false
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss} [%thread] %-5level %logger{36} %file:%line - %msg%n"
    }
}
root(INFO, ["FILE", "STDOUT"])