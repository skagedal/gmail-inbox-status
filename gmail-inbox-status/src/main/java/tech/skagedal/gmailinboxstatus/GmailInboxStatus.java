package tech.skagedal.gmailinboxstatus;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListThreadsResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.UsageMessageSpec;

public class GmailInboxStatus {
    private final JsonFactory jsonFactory = new GsonFactory();
    private final NetHttpTransport httpTransport = createHttpTransport();

    private final String account;

    public GmailInboxStatus(String account) {
        this.account = account;
    }

    public static void main(String[] args) {
        CommandSpec spec = CommandSpec.create()
            .name("gmail-inbox-status")
            .version("0.1.0")
            .usageMessage(usageMessage())
            .mixinStandardHelpOptions(true)
            .addOption(OptionSpec
                .builder("-a", "--account")
                .description("An identifier for the account you want to check.")
                .type(String.class)
                .build());
        CommandLine commandLine = new CommandLine(spec);
        commandLine.setExecutionStrategy(GmailInboxStatus::run);
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    private static UsageMessageSpec usageMessage() {
        return new UsageMessageSpec()
            .description("Prints the number of messages in a Gmail inbox");
    }

    private static int run(CommandLine.ParseResult parseResult) {
        final var helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) {
            return helpExitCode;
        }

        final var account = parseResult.matchedOptionValue('a', "default");
        return new GmailInboxStatus(account).run();
    }

    public int run() {
        final var credential = getCredential(account);
        final var service = buildGmailService(credential);
        final var response = getInboxThreads(service);
        System.out.println(response.getResultSizeEstimate());
        return 0;
    }

    // Service

    private Gmail buildGmailService(Credential credential) {
        return new Gmail.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("simons-assistant")
            .build();
    }

    private ListThreadsResponse getInboxThreads(Gmail service) {
        try {
            return service
                .users()
                .threads()
                .list("me")
                .setQ("in:inbox")
                .execute();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    // Handle credentials

    private Credential getCredential(String account) {
        final var clientSecrets = loadSecrets();
        final var flow = buildFlow(clientSecrets);

        // It would be great if we could make it open up the Google login screen with the right account pre-selected,
        // but I can't figure out how to do that.
        final var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        try {
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize(account);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private GoogleClientSecrets loadSecrets() {
        try (final var reader = Files.newBufferedReader(Paths.credentialsFile)) {
            return GoogleClientSecrets.load(jsonFactory, reader);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private GoogleAuthorizationCodeFlow buildFlow(GoogleClientSecrets secrets) {
        try {
            return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, secrets, List.of(GmailScopes.GMAIL_READONLY)
            )
                .setDataStoreFactory(new FileDataStoreFactory(Paths.tokensDirectoryPath.toFile()))
                .setAccessType("offline")
                .build();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    // HTTP transport

    private static NetHttpTransport createHttpTransport() {
        try {
            return GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
