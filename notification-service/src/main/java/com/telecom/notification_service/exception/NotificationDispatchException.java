package com.telecom.notification_service.exception;

public class NotificationDispatchException extends RuntimeException {

	public NotificationDispatchException(String message) {
		super(message);
	}

	public NotificationDispatchException(String message, Throwable cause) {
		super(message, cause);
	}
}
