package com.example.feignlogger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.feignlogger.client.SampleServiceClient;

@SpringBootApplication
@EnableFeignClients
@RestController
public class FeignLoggerApplication {

	@Autowired
	private SampleServiceClient serviceClient;

	public static void main(String[] args) {
		SpringApplication.run(FeignLoggerApplication.class, args);
	}

	@RequestMapping(produces = { MediaType.APPLICATION_JSON_VALUE }, value = "/", method = RequestMethod.GET)
	public String test() {
		return serviceClient.getData().getBody().toString();
	}






}
