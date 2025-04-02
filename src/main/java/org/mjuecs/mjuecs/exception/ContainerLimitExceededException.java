package org.mjuecs.mjuecs.exception;

public class ContainerLimitExceededException extends RuntimeException {
    public ContainerLimitExceededException(String message) {
        super(message);
    }
}