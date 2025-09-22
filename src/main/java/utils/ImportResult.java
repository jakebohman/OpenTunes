package utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.Song;

/**
 * Result wrapper for import operations.
 */
public class ImportResult {

    private final List<Song> successes = new ArrayList<>();
    private final List<ImportFailure> failures = new ArrayList<>();

    public void addSuccess(Song s) {
        if (s != null) {
            successes.add(s);
    
        }}

    public void addFailure(File f, String reason) {
        failures.add(new ImportFailure(f, reason));
    }

    public List<Song> getSuccesses() {
        return Collections.unmodifiableList(successes);
    }

    public List<ImportFailure> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    public int successCount() {
        return successes.size();
    }

    public int failureCount() {
        return failures.size();
    }

    public static class ImportFailure {

        private final File file;
        private final String reason;

        public ImportFailure(File file, String reason) {
            this.file = file;
            this.reason = reason;
        }

        public File getFile() {
            return file;
        }

        public String getReason() {
            return reason;
        }
    }
}
