package com.example;

import com.example.demo.EventStreamResponse;
import com.example.demo.Notification;
import com.example.demo.RemoteService;
import com.example.demo.RequestProcessor;
import com.example.demo.Response;
import com.example.demo.Result;
import com.example.demo.SingleResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RequestProcessorTest {

  @Test
  void returnsSingleResponseIfFirstIsResult() {
    Sinks.Many<Object> sink = Sinks.many().multicast().onBackpressureBuffer();

    // Simulate hot stream with first item being a Result
    Flux<Object> flux = sink.asFlux();
    RemoteService fakeService = new RemoteService();
    RequestProcessor processor = new RequestProcessor(new com.example.demo.RemoteService());

    // Emit the Result before calling post (so .next().block() can get it)
    sink.tryEmitNext(new Result("only result"));

    Response response = processor.post("query");

    assertTrue(response instanceof SingleResponse);
    assertEquals("only result", ((SingleResponse) response).getResult().value());
  }

  @Test
  void returnsEventStreamResponseIfFirstIsNotification() {
    Sinks.Many<Object> sink = Sinks.many().multicast().onBackpressureBuffer();

    Flux<Object> flux = sink.asFlux();
    RemoteService fakeService = new RemoteService();
    RequestProcessor processor = new RequestProcessor(fakeService);

    // Emit first Notification
    sink.tryEmitNext(new Notification("n1"));

    // Call post() — it will block on first element
    Response response = processor.post("query");

    assertTrue(response instanceof EventStreamResponse);
    EventStreamResponse stream = (EventStreamResponse) response;

    // Emit more events after post() returns
    sink.tryEmitNext(new Notification("n2"));
    sink.tryEmitNext(new Result("final result"));

    // Wait a bit for background processing to occur
    try {
      Thread.sleep(100); // better replaced with Awaitility in production
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    List<Object> events = stream.getEvents();
    assertEquals(3, events.size());
    assertTrue(events.get(0) instanceof Notification);
    assertTrue(events.get(1) instanceof Notification);
    assertTrue(events.get(2) instanceof Result);
  }

}
