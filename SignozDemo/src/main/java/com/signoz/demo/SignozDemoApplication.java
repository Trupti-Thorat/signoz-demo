package com.signoz.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SignozDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SignozDemoApplication.class, args);
	}
	
	

}
