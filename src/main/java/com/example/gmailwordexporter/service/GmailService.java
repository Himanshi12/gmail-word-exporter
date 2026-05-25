package com.example.gmailwordexporter.service;

import com.example.gmailwordexporter.config.GmailProperties;
import com.example.gmailwordexporter.dto.EmailDto;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class GmailService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_RESOURCE = "credentials.json";
    private static final String USER = "me";

    private final GmailProperties gmailProperties;
    private final ZoneId exportZoneId;

    public GmailService(
            GmailProperties gmailProperties,
            @Value("${app.export-time-zone:Asia/Kolkata}") String exportTimeZone
    ) {
        this.gmailProperties = gmailProperties;
        this.exportZoneId = ZoneId.of(exportTimeZone);
    }

    public List<EmailDto> fetchEmails(LocalDateTime startDateTime, LocalDateTime endDateTime)
            throws IOException, GeneralSecurityException {

        System.out.println("Creating Gmail client...");
        Gmail gmail = createGmailClient();
        String query = buildDateRangeQuery(startDateTime, endDateTime);
        System.out.println("Gmail query: " + query);

        long startMillis = toEpochMillis(startDateTime);
        long endMillis = toEpochMillis(endDateTime);
        List<EmailDto> emails = new ArrayList<>();
        String pageToken = null;

        do {
            ListMessagesResponse response = gmail.users()
                    .messages()
                    .list(USER)
                    .setQ(query)
                    .setPageToken(pageToken)
                    .execute();

            if (response.getMessages() != null) {
                for (Message message : response.getMessages()) {
                    EmailDto email = fetchEmail(gmail, message.getId(), startMillis, endMillis);

                    if (email != null) {
                        emails.add(email);
                    }
                }
            }

            pageToken = response.getNextPageToken();
        } while (pageToken != null);

        return emails;
    }

    private Gmail createGmailClient() throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getStoredCredential(httpTransport);

        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(gmailProperties.getApplicationName())
                .build();
    }

    public String createAuthorizationUrl(String redirectUri) throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = createAuthorizationFlow(httpTransport);

        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setAccessType("offline")
                .set("prompt", "consent")
                .build();
    }

    public void saveCredentialFromCode(String code, String redirectUri) throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = createAuthorizationFlow(httpTransport);

        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        flow.createAndStoreCredential(tokenResponse, USER);
    }

    private Credential getStoredCredential(NetHttpTransport httpTransport) throws IOException {
        GoogleAuthorizationCodeFlow flow = createAuthorizationFlow(httpTransport);
        Credential credential = flow.loadCredential(USER);

        if (credential == null) {
            throw new GmailAuthorizationRequiredException("Gmail account is not authorized. Open /api/gmail/auth-url first.");
        }

        return credential;
    }

    private GoogleAuthorizationCodeFlow createAuthorizationFlow(NetHttpTransport httpTransport) throws IOException {
        GoogleClientSecrets clientSecrets = loadClientSecrets();

        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                Collections.singletonList(GmailScopes.GMAIL_READONLY)
        )
                .setDataStoreFactory(new FileDataStoreFactory(new File(gmailProperties.getTokensDirectory())))
                .setAccessType("offline")
                .build();
    }

    private GoogleClientSecrets loadClientSecrets() throws IOException {
        String googleCredentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");

        if (googleCredentialsJson != null && !googleCredentialsJson.isBlank()) {
            try (StringReader reader = new StringReader(googleCredentialsJson)) {
                return GoogleClientSecrets.load(JSON_FACTORY, reader);
            }
        }

        InputStream in = getClass().getClassLoader().getResourceAsStream(CREDENTIALS_RESOURCE);

        if (in == null) {
            throw new IOException("Gmail credentials not found. Set GOOGLE_CREDENTIALS_JSON "
                    + "or place credentials.json inside src/main/resources for local development.");
        }

        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return GoogleClientSecrets.load(JSON_FACTORY, reader);
        }
    }

    private String buildDateRangeQuery(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        return "after:" + startDateTime.toLocalDate().minusDays(1).format(formatter)
                + " before:" + endDateTime.toLocalDate().plusDays(1).format(formatter);
    }

    private long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(exportZoneId).toInstant().toEpochMilli();
    }

    private EmailDto fetchEmail(Gmail gmail, String messageId, long startMillis, long endMillis) throws IOException {
        Message fullMessage = gmail.users()
                .messages()
                .get(USER, messageId)
                .setFormat("full")
                .execute();

        Long internalDate = fullMessage.getInternalDate();

        if (internalDate == null || internalDate < startMillis || internalDate > endMillis) {
            return null;
        }

        List<MessagePartHeader> headers = fullMessage.getPayload().getHeaders();

        return new EmailDto(
                extractEmailAddress(getHeader(headers, "From")),
                getHeader(headers, "To"),
                getHeader(headers, "Subject"),
                getHeader(headers, "Date"),
                extractBody(fullMessage.getPayload())
        );
    }

    private String extractBody(MessagePart payload) {
        String plainText = findBodyByMimeType(payload, "text/plain");

        if (!plainText.isBlank()) {
            return plainText;
        }

        String htmlText = findBodyByMimeType(payload, "text/html");

        if (!htmlText.isBlank()) {
            return htmlToText(htmlText);
        }

        return "";
    }

    private String findBodyByMimeType(MessagePart part, String mimeType) {
        if (part == null) {
            return "";
        }

        if (mimeType.equalsIgnoreCase(part.getMimeType())) {
            String decodedBody = decodeBody(part.getBody());

            if (!decodedBody.isBlank()) {
                return decodedBody;
            }
        }

        if (part.getParts() == null) {
            return "";
        }

        for (MessagePart childPart : part.getParts()) {
            String body = findBodyByMimeType(childPart, mimeType);

            if (!body.isBlank()) {
                return body;
            }
        }

        return "";
    }

    private String decodeBody(MessagePartBody body) {
        if (body == null || body.getData() == null || body.getData().isBlank()) {
            return "";
        }

        byte[] decodedBytes = Base64.getUrlDecoder().decode(body.getData());
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private String htmlToText(String html) {
        return html
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)<style.*?</style>", "")
                .replaceAll("(?is)<script.*?</script>", "")
                .replaceAll("(?is)<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .trim();
    }

    private String extractEmailAddress(String fromHeader) {
        if (fromHeader == null || fromHeader.isBlank()) {
            return "";
        }

        int start = fromHeader.indexOf('<');
        int end = fromHeader.indexOf('>');

        if (start >= 0 && end > start) {
            return fromHeader.substring(start + 1, end).trim();
        }

        return fromHeader.trim();
    }

    private String getHeader(List<MessagePartHeader> headers, String name) {
        return headers.stream()
                .filter(header -> name.equalsIgnoreCase(header.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("");
    }
}
