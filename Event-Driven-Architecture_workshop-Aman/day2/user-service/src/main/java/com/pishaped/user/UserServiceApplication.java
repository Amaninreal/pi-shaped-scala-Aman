package com.pishaped.user;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.TimeUnit;
import java.util.Random;

@SpringBootApplication
@RequiredArgsConstructor
public class UserServiceApplication implements CommandLineRunner {

    private final UserEventProducer userEventProducer;
    private final Random random = new Random();

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        for (int i = 0; i < 10; i++) {
            String userId = String.valueOf(1000 + random.nextInt(9000));

            userEventProducer.sendLoginEvent(userId);
            TimeUnit.SECONDS.sleep(3);
        }
    }
}