package com.eagle.fusex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FusexApplication {

    public static void main(String[] args) {
        SpringApplication.run(FusexApplication.class, args);
    }
}
