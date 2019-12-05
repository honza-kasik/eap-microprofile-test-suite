package org.jboss.eap.qe.microprofile.tooling.server.logwatch;

import java.util.regex.Pattern;

/**
 * Interface describing log watcher which checks log for a pattern.
 */
public interface LogWatcher {

    /**
     * Perform search in log or its excerpt and find if a line matches the pattern.
     * @param pattern a pattern which will be the log line matched against
     * @return true if log line matching pattern was found in log, false otherwise
     */
    boolean wasLineWithPatternLogged(final Pattern pattern);

}
