package com.example;

import com.example.demo.Notification;
import com.example.demo.RemoteService;
import com.example.demo.Result;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public class AnotherMain {


  public static void main(String[] args) throws InterruptedException {
    Logger logger = LoggerFactory.getLogger(AnotherMain.class);
    RemoteService remoteService = new RemoteService();
    Flux<Object> flux = remoteService.doSomething().delayElements(Duration.ofMillis(1000));
    CompletableFuture<String> future = new CompletableFuture<>();

    flux.takeUntil(o -> o instanceof Result)
        .index()
        .subscribe(tuple -> {
          long index = tuple.getT1();
          Object value = tuple.getT2();
          if (index == 0) {
            if(value instanceof Result) {
              future.complete("single");
            } else if( value instanceof Notification) {
              future.complete("stream");
            } else {
              future.complete("error");
            }
          }
          logger.info("{} | {}", index, value);
        });

    var result = future.join();
    logger.info("Result type {}", result);

    Thread.sleep(10_000);
  }

}

