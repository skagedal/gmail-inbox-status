package tech.skagedal.gmailinboxstatus;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Paths {
    private static final FileSystem fileSystem = FileSystems.getDefault();
    private static final Path home = fileSystem.getPath(System.getProperty("user.home"));
    private static final Path assistantDirectory = home.resolve(".simons-assistant");

    public static final Path credentialsFile = assistantDirectory.resolve("google-oauth-credentials.json");
    public static final Path tokensDirectoryPath = assistantDirectory.resolve("data").resolve("tokens");
}
