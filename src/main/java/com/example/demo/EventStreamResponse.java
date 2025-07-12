package com.example.demo;

import java.util.ArrayList;
import java.util.List;

public class EventStreamResponse implements Response {

  private List<Object> events = new ArrayList<>();
  public EventStreamResponse() {}

  void onEvent(Object object ){
    events.add(object);
  }

  void onComplete() {
    // call sse emitter to close connecion
  }

  void onError() {
    // log error and close SSE connection
  }

  public List<Object> getEvents() {
    return events;
  }
}
