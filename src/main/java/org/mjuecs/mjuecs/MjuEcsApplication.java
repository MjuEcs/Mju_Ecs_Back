package org.mjuecs.mjuecs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MjuEcsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MjuEcsApplication.class, args);
    }

}
