package com.solarianofc.archfixtures.rules;

import java.util.logging.Logger;

/** Fixture for the "no java.util.logging" rule (D-116): deliberately obtains a JUL logger. */
public class JavaUtilLogging {

    public Logger getLogger() {
        return Logger.getLogger(JavaUtilLogging.class.getName());
    }
}
