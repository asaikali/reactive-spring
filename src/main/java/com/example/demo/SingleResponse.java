package com.example.demo;

public final class SingleResponse implements Response {
  private  Result result;
  public SingleResponse(Result result) {
    this.result = result;
  }

  public SingleResponse() {

  }
  public SingleResponse setResult(Result result) {
    this.result = result;
    return this;
  }

  public Result getResult() {
    return result;
  }
}
