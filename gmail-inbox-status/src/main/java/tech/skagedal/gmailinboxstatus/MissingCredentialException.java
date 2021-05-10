package tech.skagedal.gmailinboxstatus;

import java.nio.file.Path;

public class MissingCredentialException extends GmailInboxStatusException {
    public final Path expectedPath;

    public MissingCredentialException(Path expectedPath) {
        this.expectedPath = expectedPath;
    }
}
