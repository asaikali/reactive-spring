package com.example.mono;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class MonoTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  void monoSubscriber() {
    Mono<String> mono = Mono.just("Hello")
        .log();

    Disposable disposableMono = mono.subscribe();

    logger.info("--------------------------------------");
    StepVerifier.create(mono)
        .expectNext("Hello")
        .verifyComplete();
  }

  @Test
  void monoSubscriberConsumer() {
    Mono<String> mono = Mono.just("Hello")
        .log();

    mono.subscribe(s -> logger.info("Value {}", s));

    logger.info("--------------------------------------");
    StepVerifier.create(mono)
        .expectNext("Hello")
        .verifyComplete();
  }

  @Test
  void monoSubscriberConsumerError() {
    Mono<String> mono = Mono.just("Hello")
        .map(s -> {
          throw new RuntimeException("error");
        });

    mono.subscribe(s -> logger.info("Value {}", s), e -> logger.error(e.getMessage()));
    mono.subscribe(s -> logger.info("Value {}", s), Throwable::printStackTrace);

    logger.info("--------------------------------------");
    StepVerifier.create(mono)
        .expectError(RuntimeException.class)
        .verify();

  }

  @Test
  void monoSubscribeConsumerComplete() {
    Mono<String> mono = Mono.just("Hello")
        .map(s -> s.toUpperCase())
        .log();

    mono.subscribe(s -> logger.info("Value {}", s), Throwable::printStackTrace,
        () -> logger.info("Complete"));

    logger.info("--------------------------------------");
    StepVerifier.create(mono)
        .expectNext("HELLO")
        .verifyComplete();
  }

  @Test
  void monoSubscribeConsumerSubscription() {
    Mono<String> mono = Mono.just("Hello")
        .map(s -> s.toUpperCase())
        .log();

    mono.subscribe(s -> logger.info("Value {}", s),
        Throwable::printStackTrace, () -> logger.info("published completed")
        , subscription -> {
          logger.info(
              "subscription started, signaling demand by calling request() on the subscription");
          // if you comment out this line you will see no data flow through the publisher
          // because by default, a publisher does not start publishing until the subscriber signals
          // that it is ready to start processing events.
          subscription.request(1);
        }
    );


    logger.info("--------------------------------------");
    StepVerifier.create(mono)
        .expectNext("HELLO")
        .verifyComplete();
  }

  @Test
  void monoDoOnMethods() {
    Mono<String> mono = Mono.just("Hello")
        //.log()

        .doOnSubscribe(s -> logger.info("doOnSubscribe1 {} ", s))
        .doOnRequest(n -> logger.info("doOnRequest Requested {}", n))
        .doOnNext(s -> logger.info("doOnNext 1 {}", s))
        .map(String::toUpperCase)
        .doOnSubscribe(s -> logger.info("doOnSubscribe2 {} ", s))
        .doOnNext(s -> logger.info("doOnNext 2 {}", s))
        .doOnSuccess(s -> logger.info("doOnSuccess on {} items", s));

    mono.subscribe(s -> logger.info("Value {}", s));

  }

  @Test
  void monoDoOnError() {
    Mono<Object> mono = Mono.error(new RuntimeException("ooo"))
        .doOnError(e -> logger.info("doOnError {} ", e.getMessage()))
        .log();


    StepVerifier.create(mono)
        .expectError(RuntimeException.class)
        .verify();

  }

  @Test
  void monoOnErrorResume() {
    Mono<Object> mono = Mono.error(new RuntimeException("ooo"))
        .onErrorResume(e -> Mono.just(e.getMessage().toUpperCase()))
        .log();


    StepVerifier.create(mono)
        .expectNext("OOO")
        .verifyComplete();
  }


  @Test
  void monoOnErrorReturn() {
    Mono<Object> mono = Mono.error(new RuntimeException("ooo"))
        .onErrorReturn( "fallback")
        .log();


    StepVerifier.create(mono)
        .expectNext("fallback")
        .verifyComplete();
  }
}
