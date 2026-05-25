package com.example.gmailwordexporter.service;

public class GmailAuthorizationRequiredException extends RuntimeException {

    public GmailAuthorizationRequiredException(String message) {
        super(message);
    }
}
