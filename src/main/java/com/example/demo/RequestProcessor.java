package com.example.demo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

public class RequestProcessor {

  private final RemoteService remoteService;

  public RequestProcessor(RemoteService remoteService) {
    this.remoteService = remoteService;
  }

  public Response post(String message) {
    Flux<Object> flux = remoteService.doSomething();

    AtomicReference<SimpleResponse> responseRef = new AtomicReference<>();

    flux.index()
        .takeWhile(tuple -> {
          Long index = tuple.getT1();
          Object o = tuple.getT2();

          if (index == 0) {
            // Process first element
            if (o instanceof Result result) {
              SimpleResponse response = new SimpleResponse(result);
              responseRef.set(response);
              return false; // Stop for single response
            } else if (o instanceof Notification notification) {
              SimpleResponse response = new SimpleResponse(notification);
              response.event(notification);
              responseRef.set(response);
              return true; // Continue for streaming
            }
            return false;
          } else {
            // Process subsequent elements (only for streaming)
            SimpleResponse response = responseRef.get();
            if (o instanceof Result result) {
              response.event(result);
              return false;
            } else if (o instanceof Notification notification) {
              response.event(notification);
              return true;
            }
            return false;
          }
        })
        .doOnComplete(() -> responseRef.get().complete())
        .doOnError(throwable -> responseRef.get().error(throwable))
        .subscribe();

    return responseRef.get().getResponse();
  }
}

