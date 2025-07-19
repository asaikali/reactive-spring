package com.example;

import com.example.demo.EventStreamResponse;
import com.example.demo.Notification;
import com.example.demo.RemoteService;
import com.example.demo.Response;
import com.example.demo.Result;
import com.example.demo.SingleResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class McpTest {

  private RemoteService remoteService  = new RemoteService();
  private Logger logger = LoggerFactory.getLogger(McpTest.class);

  @Test
  void printAllObjects() {

    Flux<Object> flux = remoteService.doSomething();
    flux.subscribe( object -> logger.info("{}",object));
  }

  @Test
  void indexResponses() {

    Flux<Object> flux = remoteService.doSomething();
    flux.index()
        .subscribe( tuple -> {
          long index = tuple.getT1();
          Object value = tuple.getT2();
          logger.info("{} | {}", index, value);
        });
  }

  @Test
  void stopWhenResultFonud() {
    Flux<Object> flux = remoteService.doSomething();
    flux.takeUntil(o -> o instanceof Result)
        .index()
        .subscribe( tuple -> {
          long index = tuple.getT1();
          Object value = tuple.getT2();
          logger.info("{} | {}", index, value);
        });
  }

  @Test
  void DetermineResultType() {
    Flux<Object> flux = remoteService.doSomething();
    CompletableFuture<String> future = new CompletableFuture<>();

    flux.takeUntil(o -> o instanceof Result)
        .index()
        .subscribe( tuple -> {
          long index = tuple.getT1();
          Object value = tuple.getT2();
          if( index == 0 && value instanceof Result result) {
            future.complete("single");
          } else {
            future.complete( "stream");
          }
          logger.info("{} | {}", index, value);
        });

    var result = future.join();
    System.out.println(result);

  }


}
