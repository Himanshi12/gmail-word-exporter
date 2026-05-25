package com.example.gmailwordexporter.repository;

import com.example.gmailwordexporter.entity.EmailExportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailExportLogRepository extends JpaRepository<EmailExportLog, Long> {
}
