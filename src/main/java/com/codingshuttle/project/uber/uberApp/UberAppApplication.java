package com.codingshuttle.project.uber.uberApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableAsync
public class UberAppApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(UberAppApplication.class, args);
	}

}
