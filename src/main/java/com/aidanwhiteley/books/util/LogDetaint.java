package com.aidanwhiteley.books.util;

public class LogDetaint {

    private LogDetaint() {}

    /*
        User provided data, such as URL parameters, POST data payloads or cookies, should always be considered untrusted and tainted.
        Applications logging tainted data could enable an attacker to inject characters that would break the log file pattern.
        This could be used to block monitors and SIEM (Security Information and Event Management) systems from detecting other malicious events.
        This problem could be mitigated by sanitizing the user provided data before logging it.
     */
    public static String logMessageDetaint(Object o) {
        return o.toString().replaceAll("[\n|\r|\t]", "_");
    }
}
