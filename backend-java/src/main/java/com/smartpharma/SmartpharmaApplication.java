package com.smartpharma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Entry point for the Spring Boot backend (inventory, auth, orders, analytics).
// The ML forecasting itself lives in a separate FastAPI service (ml-service-python/),
// called over HTTP from PredictionClient - this app never runs the model directly.
@SpringBootApplication
public class SmartpharmaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartpharmaApplication.class, args);
	}

}
