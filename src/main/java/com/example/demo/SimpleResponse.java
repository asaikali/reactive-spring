package com.example.demo;

/**
 * Package-private response builder for RequestProcessor only.
 * Usage:
 * - For single responses: new SimpleResponse(result) + firstEventIsResult(result)
 * - For streaming: new SimpleResponse(notification) + event(notification/result...)
 */
class SimpleResponse {

  private final SingleResponse singleResponse;
  private final EventStreamResponse eventStreamResponse;

  SimpleResponse(Notification notification) {
    this.eventStreamResponse = new EventStreamResponse();
    this.singleResponse = null;
  }

  SimpleResponse(Result result) {
    this.singleResponse = new SingleResponse();
    this.eventStreamResponse = null;
  }

  void firstEventIsResult(Result result) {
    this.singleResponse.setResult(result);
  }

  void event(Result result) {
    this.eventStreamResponse.onEvent(result);
    this.eventStreamResponse.complete();
  }

  void event(Notification notification) {
    this.eventStreamResponse.onEvent(notification);
  }

  Response getResponse() {
    return singleResponse != null ? singleResponse : eventStreamResponse;
  }

  void complete() {
    if (eventStreamResponse != null) {
      eventStreamResponse.complete();
    }
  }

  void error(Throwable throwable) {
    if (eventStreamResponse != null) {
      eventStreamResponse.onError(throwable);
    }
  }
}
