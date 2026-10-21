package com.example.expenses.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class Errors {
    private Errors() {}
    static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    static ResponseStatusException notFound(String m) { return new ResponseStatusException(HttpStatus.NOT_FOUND, m); }
    static ResponseStatusException conflict(String m) { return new ResponseStatusException(HttpStatus.CONFLICT, m); }
}
