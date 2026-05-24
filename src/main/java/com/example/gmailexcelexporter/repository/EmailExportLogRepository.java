package com.example.gmailexcelexporter.repository;

import com.example.gmailexcelexporter.entity.EmailExportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailExportLogRepository extends JpaRepository<EmailExportLog, Long> {
}
