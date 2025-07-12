package com.example.demo2;

import java.util.Objects;

public class DynamicResponse  {

  final SingleResponse singleResponse;
  final EventStreamResponse eventStreamResponse;

  private DynamicResponse(SingleResponse singleResponse) {
    this.singleResponse = singleResponse;
    this.eventStreamResponse = null;
  }

  private DynamicResponse(EventStreamResponse eventStreamResponse) {
    this.eventStreamResponse = eventStreamResponse;
    this.singleResponse = null;
  }

  public static DynamicResponse forSingle() {
    return new DynamicResponse(new SingleResponse());
  }

  public static DynamicResponse forStreaming() {
    return new DynamicResponse(new EventStreamResponse());
  }


  public void result(Result result) {
    if(this.singleResponse != null) {
      this.singleResponse.setResult(result);
    } else  {
      this.eventStreamResponse.onEvent(result);
      this.eventStreamResponse.complete();
    }
  }

  public void notification(Notification notification) {
    Objects.requireNonNull(eventStreamResponse);
    this.eventStreamResponse.onEvent(notification);
  }

  public void error(Throwable throwable) {
    if (eventStreamResponse != null) {
      eventStreamResponse.onError();
    }
    // Single responses might not need error handling since they're complete
  }

  public Response getResponse() {
    return singleResponse != null ? singleResponse : eventStreamResponse;
  }

  public void complete() {
    if (eventStreamResponse != null) {
      eventStreamResponse.complete();
    }
  }

  public boolean isSingle() {
    return singleResponse != null;
  }
}
