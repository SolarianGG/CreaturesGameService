package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** NullAway skips packages without {@code @NullMarked} silently, so every main package must declare it (D-64). */
class NullMarkedPackagesTest {

    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    @Test
    void everyMainPackageIsNullMarked() throws IOException {
        assertThat(packagesWithoutNullMarked())
                .as("packages without package-info.java annotated with @NullMarked")
                .isEmpty();
    }

    private static List<String> packagesWithoutNullMarked() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            return files.filter(file -> file.toString().endsWith(".java"))
                    .map(Path::getParent)
                    .distinct()
                    .filter(dir -> !isNullMarked(dir.resolve("package-info.java")))
                    .map(dir -> MAIN_SOURCES.relativize(dir).toString().replace(File.separatorChar, '.'))
                    .sorted()
                    .toList();
        }
    }

    private static boolean isNullMarked(Path packageInfo) {
        if (!Files.isRegularFile(packageInfo)) {
            return false;
        }
        try {
            return Files.readString(packageInfo).contains("@NullMarked");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
