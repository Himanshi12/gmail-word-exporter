package com.example.gmailwordexporter.controller;

import com.example.gmailwordexporter.dto.EmailExportRequest;
import com.example.gmailwordexporter.service.EmailWordExportService;
import com.example.gmailwordexporter.service.GmailAuthorizationRequiredException;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
public class EmailWordExportController {

    private final EmailWordExportService emailWordExportService;

    public EmailWordExportController(EmailWordExportService emailWordExportService) {
        this.emailWordExportService = emailWordExportService;
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportEmails(@Valid @RequestBody EmailExportRequest request)
            throws IOException, GeneralSecurityException {

        System.out.println("Export API called with startDateTime=" + request.getStartDateTime()
                + ", endDateTime=" + request.getEndDateTime());

        EmailWordExportService.EmailExportResult result = emailWordExportService.exportEmails(
                request.getStartDateTime(),
                request.getEndDateTime()
        );

        return buildWordResponse(result);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEmailsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime
    ) throws IOException, GeneralSecurityException {

        System.out.println("Export API called with startDateTime=" + startDateTime + ", endDateTime=" + endDateTime);

        EmailWordExportService.EmailExportResult result = emailWordExportService.exportEmails(startDateTime, endDateTime);

        return buildWordResponse(result);
    }

    private ResponseEntity<byte[]> buildWordResponse(EmailWordExportService.EmailExportResult result) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(result.content().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(result.fileName())
                                .build()
                                .toString()
                )
                .body(result.content());
    }

    @ExceptionHandler(GmailAuthorizationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleGmailAuthorizationRequired(
            GmailAuthorizationRequiredException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", exception.getMessage()));
    }
}
