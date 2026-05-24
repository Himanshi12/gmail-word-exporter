package com.example.gmailexcelexporter.service;

public class GmailAuthorizationRequiredException extends RuntimeException {

    public GmailAuthorizationRequiredException(String message) {
        super(message);
    }
}
