package com.pickview.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicMediaFileTest {

    @Test
    void failedCopyPreservesPublishedFileAndRemovesStagingFile(@TempDir Path directory) throws Exception {
        Path destination = directory.resolve("video.mp4");
        Files.writeString(destination, "complete");
        try (InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Interrupted download");
            }
        }) {
            assertThrows(IOException.class, () -> AtomicMediaFile.publish(broken, destination));
        }
        assertEquals("complete", Files.readString(destination));
        try (java.util.stream.Stream<Path> files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
        try (InputStream replacement = new ByteArrayInputStream("replacement".getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            AtomicMediaFile.publish(replacement, destination);
        }
        assertEquals("replacement", Files.readString(destination));
    }
}
