package com.example.demo;

public sealed interface Response permits SingleResponse,EventStreamResponse {
}
