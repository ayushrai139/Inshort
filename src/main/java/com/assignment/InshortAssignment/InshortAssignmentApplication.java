package com.assignment.InshortAssignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class InshortAssignmentApplication {

	public static void main(String[] args) {
		SpringApplication.run(InshortAssignmentApplication.class, args);
	}

}
