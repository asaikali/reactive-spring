package com.example;

import com.example.demo.Notification;
import com.example.demo.RemoteService;
import com.example.demo.Result;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class McpTest {

  private RemoteService remoteService  = new RemoteService();
  private Logger logger = LoggerFactory.getLogger(McpTest.class);

  @Test
  void testMcpLikeResponses() {
    Flux<Object> flux = remoteService.doSomething();
    flux.log();

    flux.subscribe( object -> logger.info("{}",object));


//    StepVerifier.create(flux)
//        .expectSubscription()
//        .verifyComplete();
  }

  @Test
  void neverTakeExtraObject()
  {
    Flux<Object> flux = remoteService.doSomething();
    flux.takeUntil(object -> object instanceof Result)
        .log()
        .subscribe( object -> logger.info("{}",object));



  }


}
