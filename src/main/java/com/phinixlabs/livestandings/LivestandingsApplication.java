package com.phinixlabs.livestandings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LivestandingsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LivestandingsApplication.class, args);
	}

}
