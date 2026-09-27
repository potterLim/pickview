package com.pickview.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class TemporaryFile implements AutoCloseable {

    private final Path mPath;

    TemporaryFile(String prefix, String suffix) throws IOException {
        mPath = Files.createTempFile(prefix, suffix);
    }

    TemporaryFile(Path directory, String prefix) throws IOException {
        mPath = Files.createTempFile(directory, prefix, ".tmp");
    }

    Path getPath() {
        return mPath;
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(mPath);
    }
}
