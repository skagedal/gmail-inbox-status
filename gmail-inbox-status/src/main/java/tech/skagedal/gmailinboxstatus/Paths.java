package tech.skagedal.gmailinboxstatus;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Paths {
    private static final Path applicationDirectory = FileSystems.getDefault()
        .getPath(System.getProperty("user.home"))
        .resolve(".gmail-inbox-status");

    public static final Path credentialsFile = applicationDirectory.resolve("google-oauth-credentials.json");
    public static final Path tokensDirectoryPath = applicationDirectory.resolve("tokens");
}
