package com.concurium.utils;

import java.util.HashMap;
import java.util.Map;

public class ResponseEntity<T> {
    private final int status;
    private final T body;
    private final Map<String, String> headers;

    private ResponseEntity(int status, T body) {
        this.status = status;
        this.body = body;
        this.headers = new HashMap<>();
    }


    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(HttpStatus.OK.getCode(), body);
    }

    public static <T> ResponseEntity<T> status(HttpStatus status, T body) {
        return new ResponseEntity<>(status.getCode(), body);
    }

    public static <T> ResponseEntity<T> status(int customStatus, T body) {
        return new ResponseEntity<>(customStatus, body);
    }

    public ResponseEntity<T> header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public int getStatus() { return status; }
    public T getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }
}