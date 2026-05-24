package com.example.gmailexcelexporter.dto;

public class EmailDto {

    private final String fromAddress;
    private final String toAddress;
    private final String subject;
    private final String receivedDate;
    private final String snippet;

    public EmailDto(String fromAddress, String toAddress, String subject, String receivedDate, String snippet) {
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.subject = subject;
        this.receivedDate = receivedDate;
        this.snippet = snippet;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public String getSubject() {
        return subject;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public String getSnippet() {
        return snippet;
    }
}
