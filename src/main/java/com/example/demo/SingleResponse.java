package com.example.demo;

public class SingleResponse implements Response {
  private final Result result;
  public SingleResponse(Result result) {
    this.result = result;
  }

  public Result getResult() {
    return result;
  }
}
