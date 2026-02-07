package com.themuler.reasonance.exception;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {
    private final String code;
    private final String title;
    private final String detail;
    private final HttpStatus status;

    public DomainException(String code, String title, String detail) {
        this(code, title, detail, HttpStatus.BAD_REQUEST);
    }

    public DomainException(String code, String title, String detail, HttpStatus status) {
        super(detail);
        this.code = code;
        this.title = title;
        this.detail = detail;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
