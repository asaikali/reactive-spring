package com.example.demo2;

public sealed interface Response permits SingleResponse, EventStreamResponse {
}
