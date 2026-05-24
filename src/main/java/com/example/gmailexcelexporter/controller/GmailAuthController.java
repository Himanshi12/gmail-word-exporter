package com.example.gmailexcelexporter.controller;

import com.example.gmailexcelexporter.service.GmailService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

@RestController
public class GmailAuthController {

    private final GmailService gmailService;

    public GmailAuthController(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @GetMapping("/api/gmail/auth-url")
    public Map<String, String> getAuthorizationUrl() throws IOException, GeneralSecurityException {
        String redirectUri = getRedirectUri();
        String authorizationUrl = gmailService.createAuthorizationUrl(redirectUri);

        return Map.of("authorizationUrl", authorizationUrl);
    }

    @GetMapping(value = "/oauth2callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleOAuthCallback(@RequestParam String code)
            throws IOException, GeneralSecurityException {

        gmailService.saveCredentialFromCode(code, getRedirectUri());

        return ResponseEntity.ok("""
                <!DOCTYPE html>
                <html>
                <head><title>Gmail Connected</title></head>
                <body>
                    <h2>Gmail connected successfully.</h2>
                    <p>You can close this tab and return to the exporter.</p>
                </body>
                </html>
                """);
    }

    private String getRedirectUri() {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/oauth2callback")
                .build()
                .toUriString();
    }
}
