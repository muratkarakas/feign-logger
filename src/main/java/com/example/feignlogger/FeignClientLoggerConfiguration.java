package com.example.feignlogger;

import java.util.concurrent.TimeUnit;

import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.netflix.feign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.ConnectionPool;

@Configuration
public class FeignClientLoggerConfiguration {
	
	@Bean
	public ConnectionPool httpClientConnectionPool(FeignHttpClientProperties httpClientProperties, OkHttpClientConnectionPoolFactory connectionPoolFactory) {
		Integer maxTotalConnections = httpClientProperties.getMaxConnections();
		Long timeToLive = httpClientProperties.getTimeToLive();
		TimeUnit ttlUnit = httpClientProperties.getTimeToLiveUnit();
		return connectionPoolFactory.create(maxTotalConnections, timeToLive, ttlUnit);
	}

	@Bean
	public okhttp3.OkHttpClient client(OkHttpClientFactory httpClientFactory, ConnectionPool connectionPool, FeignHttpClientProperties httpClientProperties) {
		Boolean followRedirects = httpClientProperties.isFollowRedirects();
		Integer connectTimeout = httpClientProperties.getConnectionTimeout();
		return httpClientFactory.createBuilder(false).connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).followRedirects(followRedirects).connectionPool(connectionPool).addInterceptor(new HttpLoggerInterceptor()).build();
	}
	

}
