package com.example.cinema.application;

import java.util.UUID;

public class TestUtils {
  public static String randomId() {
    return UUID.randomUUID().toString().substring(0, 7);
  }
}
