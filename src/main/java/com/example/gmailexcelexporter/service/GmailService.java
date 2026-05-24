package com.example.gmailexcelexporter.service;

import com.example.gmailexcelexporter.config.GmailProperties;
import com.example.gmailexcelexporter.dto.EmailDto;
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
import com.google.api.services.gmail.model.MessagePartHeader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class GmailService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_RESOURCE = "credentials.json";
    private static final String USER = "me";

    private final GmailProperties gmailProperties;

    public GmailService(GmailProperties gmailProperties) {
        this.gmailProperties = gmailProperties;
    }

    public List<EmailDto> fetchEmails(LocalDate startDate, LocalDate endDate)
            throws IOException, GeneralSecurityException {

        System.out.println("Creating Gmail client...");
        Gmail gmail = createGmailClient();
        String query = buildDateRangeQuery(startDate, endDate);
        System.out.println("Gmail query: " + query);

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
                    emails.add(fetchEmail(gmail, message.getId()));
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

    private String buildDateRangeQuery(LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        return "after:" + startDate.format(formatter)
                + " before:" + endDate.plusDays(1).format(formatter);
    }

    private EmailDto fetchEmail(Gmail gmail, String messageId) throws IOException {
        Message fullMessage = gmail.users()
                .messages()
                .get(USER, messageId)
                .setFormat("metadata")
                .setMetadataHeaders(Arrays.asList("From", "To", "Subject", "Date"))
                .execute();

        List<MessagePartHeader> headers = fullMessage.getPayload().getHeaders();

        return new EmailDto(
                getHeader(headers, "From"),
                getHeader(headers, "To"),
                getHeader(headers, "Subject"),
                getHeader(headers, "Date"),
                fullMessage.getSnippet()
        );
    }

    private String getHeader(List<MessagePartHeader> headers, String name) {
        return headers.stream()
                .filter(header -> name.equalsIgnoreCase(header.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("");
    }
}
