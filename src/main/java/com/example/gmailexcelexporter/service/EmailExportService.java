package com.example.gmailexcelexporter.service;

import com.example.gmailexcelexporter.dto.EmailDto;
import com.example.gmailexcelexporter.entity.EmailExportLog;
import com.example.gmailexcelexporter.repository.EmailExportLogRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailExportService {

    private final GmailService gmailService;
    private final ExcelExportService excelExportService;
    private final EmailExportLogRepository emailExportLogRepository;

    public EmailExportService(
            GmailService gmailService,
            ExcelExportService excelExportService,
            EmailExportLogRepository emailExportLogRepository
    ) {
        this.gmailService = gmailService;
        this.excelExportService = excelExportService;
        this.emailExportLogRepository = emailExportLogRepository;
    }

    public EmailExportResult exportEmails(LocalDate startDate, LocalDate endDate)
            throws IOException, GeneralSecurityException {

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }

        System.out.println("Fetching emails from Gmail...");
        List<EmailDto> emails = gmailService.fetchEmails(startDate, endDate);
        System.out.println("Fetched " + emails.size() + " emails from Gmail.");

        byte[] excelBytes = excelExportService.createEmailWorkbook(emails);
        String fileName = "gmail-emails-" + startDate + "-to-" + endDate + ".xlsx";

        Path savedFile = saveExcelFile(fileName, excelBytes);
        System.out.println("Excel file saved at: " + savedFile.toAbsolutePath());

        saveExportLog(startDate, endDate, emails.size(), fileName);

        return new EmailExportResult(fileName, excelBytes);
    }

    private Path saveExcelFile(String fileName, byte[] excelBytes) throws IOException {
        Path exportDirectory = Path.of("exports");
        Files.createDirectories(exportDirectory);

        Path outputFile = exportDirectory.resolve(fileName);
        Files.write(outputFile, excelBytes);

        return outputFile;
    }

    private void saveExportLog(LocalDate startDate, LocalDate endDate, int emailCount, String fileName) {
        EmailExportLog log = new EmailExportLog();
        log.setStartDate(startDate);
        log.setEndDate(endDate);
        log.setEmailCount(emailCount);
        log.setFileName(fileName);
        log.setExportedAt(LocalDateTime.now());

        emailExportLogRepository.save(log);
    }

    public record EmailExportResult(String fileName, byte[] content) {
    }
}
