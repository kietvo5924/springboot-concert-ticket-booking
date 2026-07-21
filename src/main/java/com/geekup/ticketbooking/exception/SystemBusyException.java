package com.geekup.ticketbooking.exception;

public class SystemBusyException extends RuntimeException {
    public SystemBusyException(String message) {
        super(message);
    }
}
