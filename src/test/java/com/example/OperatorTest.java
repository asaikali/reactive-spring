package com.example;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

public class OperatorTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  void subscribeOnSimple() {
    Flux<Integer> flux = Flux.range(1, 4)
        .map(x -> {
          logger.info("Map 1 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .map(x -> {
          logger.info("Map 2  - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        });

    StepVerifier.create(flux)
        .expectSubscription()
        .expectNext(1, 2, 3, 4)
        .verifyComplete();

  }


  @Test
  void multipleSubscribeOnSimple() {
    Flux<Integer> flux = Flux.range(1, 4)
        .subscribeOn(Schedulers.boundedElastic())
        .map(x -> {
          logger.info("Map 1 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        })
        .subscribeOn(Schedulers.single())
        .map(x -> {
          logger.info("Map 2  - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        });

    StepVerifier.create(flux)
        .expectSubscription()
        .expectNext(1, 2, 3, 4)
        .verifyComplete();

  }

  @Test
  void publishOnSimple() {
    Flux<Integer> flux = Flux.range(1, 4)
        .map(x -> {
          logger.info("Map 1 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        })
        .publishOn(Schedulers.boundedElastic())
        .map(x -> {
          logger.info("Map 2 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        });

    StepVerifier.create(flux)
        .expectSubscription()
        .expectNext(1, 2, 3, 4)
        .verifyComplete();

  }


  @Test
  void multiplePublishOnSimple() {
    Flux<Integer> flux = Flux.range(1, 4)
        .publishOn(Schedulers.single())
        .map(x -> {
          logger.info("Map 1 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        })
        .publishOn(Schedulers.boundedElastic())
        .map(x -> {
          logger.info("Map 2 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        });

    StepVerifier.create(flux)
        .expectSubscription()
        .expectNext(1, 2, 3, 4)
        .verifyComplete();

  }


  @Test
  void publishAndSubscribeOn() {
    Flux<Integer> flux = Flux.range(1, 4)
        .publishOn(Schedulers.single())
        .map(x -> {
          logger.info("Map 1 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .map(x -> {
          logger.info("Map 2 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        });

    StepVerifier.create(flux)
        .expectSubscription()
        .expectNext(1, 2, 3, 4)
        .verifyComplete();

  }

  @Test
  void publishAndSubscribeOnOrderMatters() {
    Flux<Integer> flux = Flux.range(1, 4)
        .subscribeOn(Schedulers.single())
        .map(x -> {
          logger.info("Map 1 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        })
        .publishOn(Schedulers.boundedElastic())
        .map(x -> {
          logger.info("Map 2 - value {} on thread {}", x, Thread.currentThread().getName());
          return x;
        });

    StepVerifier.create(flux)
        .expectSubscription()
        .expectNext(1, 2, 3, 4)
        .verifyComplete();
  }

  @Test
  void subscribeOnIO() throws Exception {
    Mono<List<String>> list = Mono.fromCallable(() -> Files.readAllLines(Paths.get("./src/main/resources/test.txt")))
    .log()
        .subscribeOn(Schedulers.boundedElastic());

    list.subscribe(s -> logger.info("{}",s));

  }
}
