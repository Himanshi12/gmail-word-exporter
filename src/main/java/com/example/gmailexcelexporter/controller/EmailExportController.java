package com.example.gmailexcelexporter.controller;

import com.example.gmailexcelexporter.dto.EmailExportRequest;
import com.example.gmailexcelexporter.service.EmailExportService;
import com.example.gmailexcelexporter.service.GmailAuthorizationRequiredException;
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
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
public class EmailExportController {

    private final EmailExportService emailExportService;

    public EmailExportController(EmailExportService emailExportService) {
        this.emailExportService = emailExportService;
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportEmails(@Valid @RequestBody EmailExportRequest request)
            throws IOException, GeneralSecurityException {

        System.out.println("Export API called with startDate=" + request.getStartDate()
                + ", endDate=" + request.getEndDate());

        EmailExportService.EmailExportResult result = emailExportService.exportEmails(
                request.getStartDate(),
                request.getEndDate()
        );

        return buildExcelResponse(result);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEmailsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) throws IOException, GeneralSecurityException {

        System.out.println("Export API called with startDate=" + startDate + ", endDate=" + endDate);

        EmailExportService.EmailExportResult result = emailExportService.exportEmails(startDate, endDate);

        return buildExcelResponse(result);
    }

    private ResponseEntity<byte[]> buildExcelResponse(EmailExportService.EmailExportResult result) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
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
