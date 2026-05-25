package com.example.gmailwordexporter.service;

import com.example.gmailwordexporter.dto.EmailDto;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class WordExportService {

    public byte[] createEmailDocument(List<EmailDto> emails) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            createTitle(document, emails.size());

            if (emails.isEmpty()) {
                createParagraph(document, "No emails found for the selected date range.", false);
            }

            for (int i = 0; i < emails.size(); i++) {
                if (i > 0) {
                    createHorizontalLine(document);
                }

                createEmailSection(document, emails.get(i));
            }

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createTitle(XWPFDocument document, int emailCount) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun titleRun = title.createRun();
        titleRun.setText("Gmail Word Export");
        titleRun.setBold(true);
        titleRun.setFontSize(18);

        XWPFParagraph summary = document.createParagraph();
        summary.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun summaryRun = summary.createRun();
        summaryRun.setText("Total emails: " + emailCount);
        summaryRun.setFontSize(11);
    }

    private void createEmailSection(XWPFDocument document, EmailDto email) {
        createLabelValue(document, "Client", email.getFromAddress());

        createBodyParagraphs(document, email.getBody());
    }

    private void createLabelValue(XWPFDocument document, String label, String value) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(80);

        XWPFRun labelRun = paragraph.createRun();
        labelRun.setText(label + ": ");

        XWPFRun valueRun = paragraph.createRun();
        valueRun.setText(valueOrDefault(value, ""));
        valueRun.setBold(true);
    }

    private void createBodyParagraphs(XWPFDocument document, String body) {
        String cleanBody = valueOrDefault(body, "(No body)");
        String[] lines = cleanBody.split("\\R");

        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }

            createParagraph(document, line.trim(), false);
        }
    }

    private void createParagraph(XWPFDocument document, String text, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(100);

        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(11);
        run.setBold(bold);
    }

    private void createHorizontalLine(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(220);
        paragraph.setSpacingAfter(220);
        paragraph.setBorderBottom(Borders.SINGLE);

        XWPFRun run = paragraph.createRun();
        run.setText("");
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
