package com.ll.simpleDb;

/** JDBC 의 checked SQLException 을 감싸는 런타임 예외. */
public class SimpleDbException extends RuntimeException {
    public SimpleDbException(String message, Throwable cause) {
        super(message, cause);
    }
}
