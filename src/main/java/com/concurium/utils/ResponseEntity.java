package com.concurium.utils;

import java.util.HashMap;
import java.util.Map;

public class ResponseEntity<T> {
    private final T body;
    private final int status;
    private final Map<String, String> headers = new HashMap<>();

    private ResponseEntity(T body, int status) {
        this.body = body;
        this.status = status;
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(body, 200);
    }

    public static <T> ResponseEntity<T> status(int status, T body) {
        return new ResponseEntity<>(body, status);
    }

    public ResponseEntity<T> header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public T getBody() { return body; }
    public int getStatus() { return status; }
    public Map<String, String> getHeaders() { return headers; }
}