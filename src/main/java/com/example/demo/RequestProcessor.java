package com.example.demo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class RequestProcessor {

  private final RemoteService remoteService;

  public RequestProcessor(RemoteService remoteService) {
    this.remoteService = remoteService;
  }

  public Response post(String message) {
    Flux<Object> flux = remoteService.doSomething();

    final SingleResponse singleResponse = new  SingleResponse();
    final EventStreamResponse eventStreamResponse = new EventStreamResponse();
    flux.index((index, o) -> {
          if (index == 0) {
            if (o instanceof Result result) {
              singleResponse.setResult(result);
            } else if (o instanceof Notification notification) {
              eventStreamResponse.onEvent(notification);
            }
          }
          return o;
        })
        .skip(1) // Skip the first element since we already processed it
        .takeWhile(o -> {
          if (o instanceof Result result) {
            if (singleResponse.getResult() != null) {
              // First element was a Result - stop immediately
              return false;
            } else {
              // Result after notifications - add to stream and stop
              eventStreamResponse.onEvent(result);
              eventStreamResponse.onComplete();
              return false;
            }
          } else if (o instanceof Notification notification) {
            eventStreamResponse.onEvent(notification);
            return true; // Continue streaming
          } else {
            return false;
          }
        })
        .doOnComplete(() -> {
          // Only call onComplete if stream ended naturally (no Result)
          if (singleResponse.getResult() == null) {
            eventStreamResponse.onComplete();
          }
        })
        .subscribe();

    // Return appropriate response
    return singleResponse.getResult() != null ? singleResponse : eventStreamResponse;
  }

  public Response post7(String query) {
    AtomicReference<EventStreamResponse> streamRef = new AtomicReference<>();

    return (Response) remoteService.doSomething()
        .cast(Object.class)
        .handle((element, sink) -> {
          if (element instanceof Result result) {
            sink.next(new SingleResponse(result));
            sink.complete(); // Terminates stream
          } else if (element instanceof Notification notification) {
            EventStreamResponse stream = streamRef.updateAndGet(existing -> {
              if (existing == null) {
                existing = new EventStreamResponse();
              }
              existing.onEvent(notification);
              return existing;
            });
            sink.next(stream);
          }
          // Ignore other types
        })
        .doOnError(throwable -> {
          EventStreamResponse stream = streamRef.get();
          if (stream != null) stream.onError();
        })
        .doOnComplete(() -> {
          EventStreamResponse stream = streamRef.get();
          if (stream != null) stream.onComplete();
        })
        .blockFirst();
  }

  public Response post6(String query) {
    Flux<Object> flux = remoteService.doSomething(); // Hot flux

    AtomicReference<Response> responseRef = new AtomicReference<>();
    CountDownLatch firstElementLatch = new CountDownLatch(1);

    flux.index()
        .takeUntil(tuple -> {
          Long index = tuple.getT1();
          Object element = tuple.getT2();

          if (index == 0) {
            // First element - decide response type and whether to continue
            if (element instanceof Result result) {
              responseRef.set(new SingleResponse(result));
              firstElementLatch.countDown();
              return true; // Stop here - this will cancel the flux
            } else {
              EventStreamResponse streamResponse = new EventStreamResponse();
              streamResponse.onEvent(element);
              responseRef.set(streamResponse);
              firstElementLatch.countDown();
              return false; // Continue streaming
            }
          } else {
            // Subsequent elements - continue streaming
            Response response = responseRef.get();
            if (response instanceof EventStreamResponse streamResponse) {
              streamResponse.onEvent(element);
            }
            return false; // Keep going
          }
        })
        .subscribe();

    // Wait for first element to determine response type
    try {
      firstElementLatch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }

    return responseRef.get();
  }

  public Response post3(String query) {
    Flux<Object> flux = remoteService.doSomething(); // Hot flux

    AtomicReference<Response> responseRef = new AtomicReference<>();
    CountDownLatch firstElementLatch = new CountDownLatch(1);

    Disposable subscription = flux.index()
        .subscribe(tuple -> {
          Long index = tuple.getT1();
          Object element = tuple.getT2();

          if (index == 0) {
            // First element - decide response type
            if (element instanceof Result result) {
              responseRef.set(new SingleResponse(result));
              firstElementLatch.countDown();
              // Don't dispose here - let the main thread handle it
            } else {
              EventStreamResponse streamResponse = new EventStreamResponse();
              streamResponse.onEvent(element);
              responseRef.set(streamResponse);
              firstElementLatch.countDown();
            }
          } else {
            // Subsequent elements - continue streaming if it's EventStreamResponse
            Response response = responseRef.get();
            if (response instanceof EventStreamResponse streamResponse) {
              streamResponse.onEvent(element);
            }
          }
        });

    // Wait for first element to determine response type
    try {
      firstElementLatch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      subscription.dispose(); // Clean up on interrupt
      throw new RuntimeException(e);
    }

    Response response = responseRef.get();

    // Cancel subscription if it's a single response
    if (response instanceof SingleResponse) {
      subscription.dispose();
    }

    return response;
  }

  public Response post2(String query) {
    Flux<Object> flux = remoteService.doSomething();

    Object first = flux.take(1).blockFirst();

    if (first instanceof Result result) {
      return new SingleResponse(result);
    }

    EventStreamResponse streamResponse = new EventStreamResponse();
    streamResponse.onEvent(first);

    flux.skip(1).subscribe(streamResponse::onEvent);

    return streamResponse;
  }


  public Response post1(String query) {
    Flux<Object> flux = remoteService.doSomething();

    return flux.index()
        .switchOnFirst((signal, indexedFlux) -> {
          if (signal.hasValue()) {
            Tuple2<Long, Object> firstTuple = signal.get();
            Object firstElement = firstTuple.getT2();

            if (firstElement instanceof Result result) {
              return Flux.just(new SingleResponse(result));
            } else {
              EventStreamResponse streamResponse = new EventStreamResponse();
              return indexedFlux
                  .doOnNext(tuple -> {
                    Object element = tuple.getT2();
                    streamResponse.onEvent(element);
                  })
                  .thenMany(Flux.just(streamResponse));
            }
          }
          return Flux.empty();
        })
        .cast(Response.class)
        .blockFirst(); // Use blockFirst() for Flux
  }
}

