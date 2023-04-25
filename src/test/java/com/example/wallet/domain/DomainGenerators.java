package com.example.wallet.domain;

import java.util.UUID;

public class DomainGenerators {
  public static String randomCommandId() {
    return UUID.randomUUID().toString();
  }
}
