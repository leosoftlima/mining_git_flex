package util


interface RegexUtil {

    final FILE_SEPARATOR_REGEX = /(\\|\/)/
    final NEW_LINE_REGEX = /\r\n|\n/
    final PIVOTAL_TRACKER_ID_REGEX = /.*\[(#|fix.* #|complet.* #|finish.* #)\d+\].*/
    final GENERAL_ID_REGEX = /.*#\d+.*/

}
