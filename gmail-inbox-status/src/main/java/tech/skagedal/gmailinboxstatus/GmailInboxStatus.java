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
import java.nio.file.NoSuchFileException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Objects;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.UsageMessageSpec;

public class GmailInboxStatus {
    private final JsonFactory jsonFactory = new GsonFactory();
    private final NetHttpTransport httpTransport = createHttpTransport();

    private final String account;
    private final boolean unread;
    private final boolean checkEmpty;

    public GmailInboxStatus(String account, boolean unread, boolean checkEmpty) {
        this.account = account;
        this.unread = unread;
        this.checkEmpty = checkEmpty;
    }

    public static void main(String[] args) {
        final var spec = CommandSpec.create()
            .name("gmail-inbox-status")
            .version("0.1.0")
            .usageMessage(usageMessage())
            .mixinStandardHelpOptions(true)
            .addOption(OptionSpec
                .builder("-a", "--account")
                .description("An identifier for the account you want to check.")
                .type(String.class)
                .build())
            .addOption(OptionSpec
                .builder("-u", "--unread")
                .description("Only count unread messages")
                .build())
            .addOption(OptionSpec
                .builder("-c", "--check-empty")
                .description("Do not output anything, only exit successfully if empty.")
                .build());

        final var commandLine = new CommandLine(spec)
            .setExecutionStrategy(GmailInboxStatus::run);

        if (shouldPrintAutoCompleteScript()) {
            printAutoCompleteScript(commandLine);
        } else {
            int exitCode = commandLine.execute(args);
            System.exit(exitCode);
        }
    }

    private static boolean shouldPrintAutoCompleteScript() {
        return Objects.equals(System.getenv("_GMAIL_INBOX_STATUS_AUTOCOMPLETE"), "true");
    }

    private static void printAutoCompleteScript(CommandLine commandLine) {
        System.out.println(AutoComplete.bash("gmail-inbox-status", commandLine));
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
        final var unread = parseResult.matchedOptionValue('u', false);
        final var checkEmpty = parseResult.matchedOptionValue('c', false);
        return new GmailInboxStatus(account, unread, checkEmpty).run();
    }

    public int run() {
        try {
            final var credential = getCredential(account);
            final var service = buildGmailService(credential);
            final var response = getInboxThreads(service);
            final var estimatedCount = response.getResultSizeEstimate();
            if (checkEmpty) {
                return (estimatedCount == 0) ? 0 : 1;
            } else {
                System.out.println(estimatedCount);
                return 0;
            }
        } catch (MissingCredentialException exception) {
            System.err.printf("Could not find a Google Auth credentials file at %s.\n", exception.expectedPath);
            System.err.println();
            System.err.println("Go to https://console.cloud.google.com/apis/credentials and create an OAuth client ID.");
            System.err.println("Download it as a JSON file and then save it to the above location.");
            return 1;
        }
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
                .setQ(query())
                .execute();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private String query() {
        return unread
            ? "in:inbox is:unread"
            : "in:inbox";
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
        final var path = Paths.credentialsFile;
        try (final var reader = Files.newBufferedReader(path)) {
            return GoogleClientSecrets.load(jsonFactory, reader);
        } catch (NoSuchFileException exception) {
            throw new MissingCredentialException(path);
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
