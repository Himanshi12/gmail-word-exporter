package com.example.gmailwordexporter.service;

import com.example.gmailwordexporter.dto.EmailDto;
import com.example.gmailwordexporter.entity.EmailExportLog;
import com.example.gmailwordexporter.repository.EmailExportLogRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailWordExportService {

    private final GmailService gmailService;
    private final WordExportService wordExportService;
    private final EmailExportLogRepository emailExportLogRepository;

    public EmailWordExportService(
            GmailService gmailService,
            WordExportService wordExportService,
            EmailExportLogRepository emailExportLogRepository
    ) {
        this.gmailService = gmailService;
        this.wordExportService = wordExportService;
        this.emailExportLogRepository = emailExportLogRepository;
    }

    public EmailExportResult exportEmails(LocalDateTime startDateTime, LocalDateTime endDateTime)
            throws IOException, GeneralSecurityException {

        if (endDateTime.isBefore(startDateTime)) {
            throw new IllegalArgumentException("endDateTime must be greater than or equal to startDateTime");
        }

        System.out.println("Fetching emails from Gmail...");
        List<EmailDto> emails = gmailService.fetchEmails(startDateTime, endDateTime);
        System.out.println("Fetched " + emails.size() + " emails from Gmail.");

        byte[] wordBytes = wordExportService.createEmailDocument(emails);
        String fileName = "gmail-emails-" + formatForFileName(startDateTime)
                + "-to-" + formatForFileName(endDateTime) + ".docx";

        Path savedFile = saveWordFile(fileName, wordBytes);
        System.out.println("Word file saved at: " + savedFile.toAbsolutePath());

        saveExportLog(startDateTime, endDateTime, emails.size(), fileName);

        return new EmailExportResult(fileName, wordBytes);
    }

    private Path saveWordFile(String fileName, byte[] wordBytes) throws IOException {
        Path exportDirectory = Path.of("exports");
        Files.createDirectories(exportDirectory);

        Path outputFile = exportDirectory.resolve(fileName);
        Files.write(outputFile, wordBytes);

        return outputFile;
    }

    private void saveExportLog(LocalDateTime startDateTime, LocalDateTime endDateTime, int emailCount, String fileName) {
        EmailExportLog log = new EmailExportLog();
        log.setStartDate(startDateTime.toLocalDate());
        log.setEndDate(endDateTime.toLocalDate());
        log.setEmailCount(emailCount);
        log.setFileName(fileName);
        log.setExportedAt(LocalDateTime.now());

        emailExportLogRepository.save(log);
    }

    private String formatForFileName(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
    }

    public record EmailExportResult(String fileName, byte[] content) {
    }
}
