package com.example.demo2;

import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Flux;

public class RequestProcessor {

  private final RemoteService remoteService;

  public RequestProcessor(RemoteService remoteService) {
    this.remoteService = remoteService;
  }

  public Response post(String message) {
    Flux<Object> flux = remoteService.doSomething();

    AtomicReference<DynamicResponse> responseRef = new AtomicReference<>();

    flux.index((index, o) -> {
          if (index == 0) {
            if (o instanceof Result result) {
              DynamicResponse response = DynamicResponse.forSingle();
              response.result(result);
              responseRef.set(response);
            } else if (o instanceof Notification notification) {
              DynamicResponse response = DynamicResponse.forStreaming();
              response.notification(notification);
              responseRef.set(response);
            }
          }
          return o;
        })
        .skip(1) // Skip the first element since we already processed it
        .takeWhile(o -> {
          DynamicResponse response = responseRef.get();
          if (response == null) return false;

          if (o instanceof Result result) {
            if (response.isSingle()) {
              return false; // First element was a Result - stop
            } else {
              response.result(result); // This calls onEvent + complete
              return false;
            }
          } else if (o instanceof Notification notification) {
            response.notification(notification);
            return true; // Continue streaming
          } else {
            return false; // Stop on unknown types
          }
        })
        .doOnComplete(() -> {
          DynamicResponse response = responseRef.get();
          if (response != null && !response.isSingle()) {
            response.complete();
          }
        })
        .doOnError(throwable -> {
          DynamicResponse response = responseRef.get();
          if (response != null) {
            response.error(throwable);
          }
        })
        .subscribe();

    return responseRef.get().getResponse();
  }
}

