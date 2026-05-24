package com.example.gmailexcelexporter.service;

import com.example.gmailexcelexporter.dto.EmailDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] createEmailWorkbook(List<EmailDto> emails) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Gmail Emails");
            createHeaderRow(workbook, sheet);
            createEmailRows(sheet, emails);

            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createHeaderRow(Workbook workbook, Sheet sheet) {
        Row row = sheet.createRow(0);
        String[] headers = {"From", "To", "Subject", "Received Date", "Snippet"};

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createEmailRows(Sheet sheet, List<EmailDto> emails) {
        int rowIndex = 1;

        for (EmailDto email : emails) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(email.getFromAddress());
            row.createCell(1).setCellValue(email.getToAddress());
            row.createCell(2).setCellValue(email.getSubject());
            row.createCell(3).setCellValue(email.getReceivedDate());
            row.createCell(4).setCellValue(email.getSnippet());
        }
    }
}
