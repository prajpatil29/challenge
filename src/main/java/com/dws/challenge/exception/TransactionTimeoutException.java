package com.dws.challenge.exception;

public class TransactionTimeoutException extends RuntimeException {

  public TransactionTimeoutException(String message) {
    super(message);
  }
}
