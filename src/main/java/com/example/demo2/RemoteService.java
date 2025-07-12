package com.example.demo2;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import reactor.core.publisher.Flux;

public class RemoteService {

  private int counter = 1;
  private final Random random = new SecureRandom();

  public Flux<Object> doSomething() {
    Result result = new Result("Result # " + counter++);

    boolean generateNotifications = random.nextBoolean();
    if (generateNotifications) {

      int notificationToGenerate = random.nextInt(1, 11);
      List<Object> response = IntStream.rangeClosed(1,notificationToGenerate)
          .mapToObj(i -> new Notification("notification #" + i)).collect(
              Collectors.toList());

      response.add(result);
      response.add("extra");

      return Flux.fromIterable(response);
    } else {
      return Flux.just(result, "extra");
    }
  }

}
