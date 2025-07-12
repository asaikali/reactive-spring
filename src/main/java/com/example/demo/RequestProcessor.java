package com.example.demo;

import reactor.core.publisher.Flux;

public class RequestProcessor {

  private final RemoteService remoteService;

  public RequestProcessor(RemoteService remoteService) {
    this.remoteService = remoteService;
  }

  public Response post(String query) {
    Flux<Object> flux = remoteService.doSomething();

    // Share the stream between first() and subscription
    Flux<Object> shared = flux.publish().autoConnect(2);

    Object first = shared.next().block(); // Block until first item arrives

    if (first instanceof Result result) {
      return new SingleResponse(result);
    }

    EventStreamResponse streamResponse = new EventStreamResponse();
    streamResponse.onEvent(first); // Include the first item

    shared.skip(1).subscribe(streamResponse::onEvent); // Process remaining items in background

    return streamResponse;
  }
}
