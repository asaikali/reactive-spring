package com.example.demo2;


public class Main {

  public static void main(String[] args) {
    RemoteService remoteService = new RemoteService();
    RequestProcessor processor = new RequestProcessor(remoteService);
    Response response = processor.post("test");
    switch (response) {
      case SingleResponse singleResponse:
        System.out.println("Got a single response result object " + singleResponse.getResult());
        break;
      case EventStreamResponse eventStreamResponse:
        System.out.println("Got an event stream result object " + eventStreamResponse.getEvents());
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + response);
    }
  }
}



