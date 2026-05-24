package com.example.gmailexcelexporter.controller;

import com.example.gmailexcelexporter.entity.EmailExportLog;
import com.example.gmailexcelexporter.repository.EmailExportLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/email-export-logs")
public class EmailExportLogController {

    private final EmailExportLogRepository emailExportLogRepository;

    public EmailExportLogController(EmailExportLogRepository emailExportLogRepository) {
        this.emailExportLogRepository = emailExportLogRepository;
    }

    @GetMapping
    public List<EmailExportLog> findAll() {
        return emailExportLogRepository.findAll();
    }
}
