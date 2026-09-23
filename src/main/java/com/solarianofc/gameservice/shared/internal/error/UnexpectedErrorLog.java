package com.solarianofc.gameservice.shared.internal.error;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one place that logs 5xx answers (D-156): at {@code ERROR} with the cause, so the trace ID in the log line links
 * it to the {@code traceId} of the generic problem the client gets.
 */
public final class UnexpectedErrorLog {

    private static final Logger LOG = LoggerFactory.getLogger(UnexpectedErrorLog.class);

    private UnexpectedErrorLog() {}

    /** {@code cause} is {@code null} when the container reports only a status, e.g. {@code sendError(500)}. */
    public static void record(String method, String path, @Nullable Throwable cause) {
        // Method and path come from the client; line breaks in them must not forge log lines.
        String safeMethod = withoutLineBreaks(method);
        String safePath = withoutLineBreaks(path);
        LOG.error("Unexpected error on {} {}", safeMethod, safePath, cause);
    }

    private static String withoutLineBreaks(String value) {
        return value.replace("\r", "_").replace("\n", "_");
    }
}
