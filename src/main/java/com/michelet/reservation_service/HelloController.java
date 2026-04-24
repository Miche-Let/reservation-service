package com.michelet.reservation_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  @GetMapping("/reservations/hello")
  public String hello() {
    return "hello from reservation-service";
  }
}