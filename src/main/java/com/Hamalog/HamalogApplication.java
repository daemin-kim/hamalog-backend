package com.Hamalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HamalogApplication {

	public static void main(String[] args) {
		SpringApplication.run(HamalogApplication.class, args);
	}

}
