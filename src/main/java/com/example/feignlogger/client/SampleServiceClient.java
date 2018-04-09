package com.example.feignlogger.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.databind.JsonNode;

@FeignClient(name="sampleserviceclient" , url="http://httpbin.org/get")
public interface SampleServiceClient {

	
	

	@RequestMapping( method = RequestMethod.GET)
	public ResponseEntity<JsonNode>  getData();
	
	
	@RequestMapping( method = RequestMethod.POST)
	public ResponseEntity<JsonNode>  postData(@RequestBody String data);

	
}
