package com.solarianofc.archfixtures.rules;

import java.io.PrintStream;

/** Fixture for the "no standard streams" rule (D-116): deliberately reads {@code System.out} (no print call). */
public class StandardStreams {

    public PrintStream getOut() {
        return System.out;
    }
}
