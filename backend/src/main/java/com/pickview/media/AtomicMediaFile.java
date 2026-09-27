package com.pickview.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class AtomicMediaFile {

    private AtomicMediaFile() {
        // File publication is stateless; callers own the input stream.
    }

    static void publish(InputStream source, Path destination) throws IOException {
        try (TemporaryFile temporary = new TemporaryFile(destination.getParent(), "pickview-cache-")) {
            Files.copy(source, temporary.getPath(), StandardCopyOption.REPLACE_EXISTING);
            // Same-directory staging keeps incomplete files invisible to readers.
            Files.move(temporary.getPath(), destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
