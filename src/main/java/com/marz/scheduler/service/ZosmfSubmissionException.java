package com.marz.scheduler.service;

public class ZosmfSubmissionException extends RuntimeException {

    public ZosmfSubmissionException(String message) {
        super(message);
    }

    public ZosmfSubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
