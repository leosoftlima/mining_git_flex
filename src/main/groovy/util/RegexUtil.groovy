package util


class RegexUtil {

    public static final FILE_SEPARATOR_REGEX = /(\\|\/)/
    public static final NEW_LINE_REGEX = /\r\n|\n/
    public static final PIVOTAL_TRACKER_ID_REGEX = /.*\[(#|fix.* #|complet.* #|finish.* #)\d+\].*/
    public static final GENERAL_ID_REGEX = /.*#\d+.*/

}
