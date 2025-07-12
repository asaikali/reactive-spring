package com.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

public class FluxTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());


  @Test
  void fluxSubscriber() {
    Flux<String> flux = Flux.just("a", "b", "c")
        .log();

    flux.subscribe(s -> logger.info("Value {}", s));

    logger.info("--------------------------------------------------");
    StepVerifier.create(flux)
        .expectNext("a", "b", "c")
        .verifyComplete();

  }

  @Test
  void fluxSubscriberNumbers() {
    Flux<Integer> flux = Flux.range(1, 3)
        .log();

    flux.subscribe(s -> logger.info("Value {}", s));

    logger.info("--------------------------------------------------");
    StepVerifier.create(flux)
        .expectNext(1, 2, 3)
        .verifyComplete();

  }

  @Test
  void fluxSubscriberFromList() {
    Flux<Integer> flux = Flux.fromIterable(List.of(1, 2, 3))
        .log();

    flux.subscribe(s -> logger.info("Value {}", s));

    logger.info("--------------------------------------------------");
    StepVerifier.create(flux)
        .expectNext(1, 2, 3)
        .verifyComplete();
  }

  @Test
  void fluxSubscriberErrors() {
    Flux<Integer> flux = Flux.range(1, 5)
        .map(i -> {
          if (i == 4) {
            throw new RuntimeException("oops");
          }
          return i;
        })
        .log();

    flux.subscribe(s -> logger.info("Value {}", s));

    logger.info("--------------------------------------------------");
    StepVerifier.create(flux)
        .expectNext(1, 2, 3)
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void fluxSubscriberUglyBackPressure() {
    Flux<Integer> flux = Flux.range(1, 10)
        .log();

    flux.subscribe(new Subscriber<Integer>() {

      private Subscription subscription;
      private int count;
      private final int n = 2;

      @Override
      public void onSubscribe(Subscription s) {
        this.subscription = s;
        subscription.request(2);
      }

      @Override
      public void onNext(Integer integer) {
        count++;
        if (count == n) {
          subscription.request(n);
          count = 0;
        }
      }

      @Override
      public void onError(Throwable t) {

      }

      @Override
      public void onComplete() {

      }
    });

    logger.info("--------------------------------------------------");
    StepVerifier.create(flux)
        .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .verifyComplete();
  }

  @Test
  void fluxBaseSubscriber() {
    Flux<Integer> flux = Flux.range(1, 10)
        .log();

    flux.subscribe(new BaseSubscriber<Integer>() {
      private int count;
      private final int n = 2;

      @Override
      protected void hookOnSubscribe(Subscription subscription) {
        subscription.request(n);
      }

      @Override
      protected void hookOnNext(Integer value) {
        count++;
        if (count == n) {
          request(n);
          count = 0;
        }
      }
    });

    logger.info("--------------------------------------------------");
    StepVerifier.create(flux)
        .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .verifyComplete();
  }

  @Test
  void fluxPrettyBackPressure() {
    Flux<Integer> flux = Flux.range(1, 10)
        .log()
        .limitRate(3);

    flux.subscribe(s -> logger.info("Value {}", s));

    logger.info("--------------------------------------------------");
    StepVerifier.create(flux)
        .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .verifyComplete();


  }

  @Test
  void fluxIntervalOne() throws InterruptedException {
    Flux<Long> flux = Flux.interval(Duration.ofMillis(100))
        .take(10)
        .log();

    flux.subscribe(s -> logger.info("Value {}", s));

    Thread.sleep(3000);
  }


  @Test
  void fluxIntervalTwo() throws InterruptedException {
    Flux<Long> flux = createFlux();

    //flux.subscribe(s -> logger.info("Value {}", s));

    StepVerifier.withVirtualTime(() -> createFlux())
        .expectSubscription()
        .thenAwait(Duration.ofDays(1))
        .expectNext(0L)
        .thenAwait(Duration.ofDays(1))
        .expectNext(1L)
        .thenCancel()
        .verify();


  }

  private Flux<Long> createFlux() {
    return Flux.interval(Duration.ofDays(1))
        .take(10)
        .log();
  }


  @Test
  void connectableFlux() throws InterruptedException {
    ConnectableFlux<Integer> connectableFlux = Flux.range(1, 10)
        //  .doOnNext(i -> logger.info("onNext: {}", i))
        .log()
        .delayElements(Duration.ofMillis(100))
        .publish();

    //   connectableFlux.connect();

//    logger.info("Thread sleeping 300ms");
//    Thread.sleep(200);
//
//    connectableFlux.subscribe( i -> logger.info("Sub1 number {}", i));
//
//     logger.info("Thread sleeping 200ms");
//     Thread.sleep(200);

    connectableFlux.subscribe(i -> logger.info("Sub2 number {}", i));

    StepVerifier.create(connectableFlux)
        .then(connectableFlux::connect)
        .thenConsumeWhile(i -> i <= 5)
        .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .expectComplete()
        .verify();
  }


  @Test
  void subscribeOnIO() throws Exception {
    Mono<List<String>> list = Mono.fromCallable(
            () -> Files.readAllLines(Path.of("./src/test/resources/test.txt")))
        .log()
        .subscribeOn(Schedulers.boundedElastic());

    list.subscribe(s -> logger.info("Value {}", s));

    //  Thread.sleep(3000);

    StepVerifier.create(list)
        .expectSubscription()
        .thenConsumeWhile(l -> {
          Assertions.assertFalse(l.isEmpty());
          logger.info("Size {}", l.size());
          return true;
        })
        .verifyComplete();
  }

  @Test
  void switchIfEmpty() {
    Flux<Object> flux = Flux.empty()
        .switchIfEmpty(Flux.just("test"))
        .log();

    StepVerifier.create(flux)
        .expectSubscription()
        .expectNext("test")
        .expectComplete()
        .verify();
  }

  @Test
  void deferTest() throws InterruptedException {
    Mono<Long> mono = Mono.just(System.currentTimeMillis());
    Mono<Long> defer = Mono.defer(() -> Mono.just(System.currentTimeMillis()));


    mono.subscribe(l -> logger.info("Value {}", l));
    defer.subscribe(l -> logger.info("defered {}", l));

    Thread.sleep(100);
    mono.subscribe(l -> logger.info("Value {}", l));
    defer.subscribe(l -> logger.info("defered {}", l));

    Thread.sleep(100);
    mono.subscribe(l -> logger.info("Value {}", l));
    defer.subscribe(l -> logger.info("defered {}", l));

    Thread.sleep(100);
    mono.subscribe(l -> logger.info("Value {}", l));
    defer.subscribe(l -> logger.info("defered {}", l));

    AtomicLong atomicLong = new AtomicLong();
    defer.subscribe(atomicLong::set);
    Assertions.assertTrue(atomicLong.get() > 0);
  }


  @Test
  void fluxContact() {
    Flux<String> flux1 = Flux.just("a","b");
    Flux<String> flux2 = Flux.just("c","d");

    Flux<String> contactFlux = Flux.concat(flux1, flux2).log();

    StepVerifier.create(contactFlux)
        .expectSubscription()
        .expectNext("a","b","c", "d")
        .verifyComplete();
  }

  @Test
  void fluxContactOperator() {
    Flux<String> flux1 = Flux.just("a","b");
    Flux<String> flux2 = Flux.just("c","d");

    Flux<String> contactFlux = flux1.concatWith(flux2).log();

    StepVerifier.create(contactFlux)
        .expectSubscription()
        .expectNext("a","b","c", "d")
        .verifyComplete();
  }

  @Test
  void combineLast() {
    Flux<String> flux1 = Flux.just("a","b");
    Flux<String> flux2 = Flux.just("c","d");

    Flux<String> combinedFlux = Flux.combineLatest( flux1, flux2, (s1, s2) -> s1.toUpperCase( )+ " " + s2.toUpperCase()).log();

    combinedFlux.subscribe(s -> logger.info("Value {}", s));
  }


  @Test
  void fluxMergeOperator() {
    Flux<String> flux1 = Flux.just("a","b").delayElements(Duration.ofMillis(200));
    Flux<String> flux2 = Flux.just("c","d");

    Flux<String> meregFlux = Flux.merge(flux1,flux2).log();

    StepVerifier.create(meregFlux)
        .expectSubscription()
        .expectNext("c", "d","a","b")
        .verifyComplete();
  }

  @Test
  void fluxMergeSequentialOperator() {
    Flux<String> flux1 = Flux.just("a","b").delayElements(Duration.ofMillis(200));
    Flux<String> flux2 = Flux.just("c","d");

    Flux<String> meregFlux = Flux.mergeSequential(flux1,flux2,flux1).log();

    StepVerifier.create(meregFlux)
        .expectSubscription()
        .expectNext("a", "b","c","d","a","b")
        .verifyComplete();
  }



  @Test
  void fluxContactError() {
    Flux<String> flux1 = Flux.just("a","b")
        .map( s -> {
          if( s.equals("b")) throw new RuntimeException();
          return s;
        });
    Flux<String> flux2 = Flux.just("c","d");

    Flux<String> contactFlux = Flux.concatDelayError(flux1, flux2).log();

    StepVerifier.create(contactFlux)
        .expectSubscription()
        .expectNext("a","b","c")
        .expectError()
        .verify();
  }
}
