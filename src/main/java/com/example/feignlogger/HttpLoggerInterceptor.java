package com.example.feignlogger;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;

public  class HttpLoggerInterceptor implements Interceptor {
	private Charset UTF8 = Charset.forName("UTF-8");
	private Logger logger = LoggerFactory.getLogger(getClass());

	private boolean bodyHasUnknownEncoding(Headers headers) {
		String contentEncoding = headers.get("Content-Encoding");
		return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity") && !contentEncoding.equalsIgnoreCase("gzip");
	}

	private boolean isPlaintext(Buffer buffer) {
	    try {
	      Buffer prefix = new Buffer();
	      long byteCount = buffer.size() < 64 ? buffer.size() : 64;
	      buffer.copyTo(prefix, 0, byteCount);
	      for (int i = 0; i < 16; i++) {
	        if (prefix.exhausted()) {
	          break;
	        }
	        int codePoint = prefix.readUtf8CodePoint();
	        if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
	          return false;
	        }
	      }
	      return true;
	    } catch (EOFException e) {
	      return false; // Truncated UTF-8 sequence.
	    }
	  }

	@Override
	public Response intercept(Chain chain) throws IOException {

		Request request = chain.request();

		RequestBody requestBody = request.body();
		boolean hasRequestBody = requestBody != null;

		Connection connection = chain.connection();
		StringBuilder logStringBuider = new StringBuilder("Request Summary : [ " + request.method() + ' ' + request.url() + (connection != null ? " " + connection.protocol() : "] ") );

		if (hasRequestBody) {
			if (requestBody.contentType() != null) {
				logStringBuider.append("Content-Type: [" + requestBody.contentType()+"] ");
			}
			if (requestBody.contentLength() != -1) {
				logStringBuider.append("Content-Length: [ " + requestBody.contentLength()+"] ");
			}
		}

		Headers headers = request.headers();
		for (int i = 0, count = headers.size(); i < count; i++) {
			String name = headers.name(i);
			// Skip headers from the request body as they are explicitly logged above.
			if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
				logStringBuider.append("requestheader :["+name + ": " + headers.value(i)+"] ");
			}
		}

		if (!hasRequestBody) {
			logStringBuider.append("requestbody:[ NONE ] ");
		} else if (bodyHasUnknownEncoding(request.headers())) {
			logStringBuider.append("requestbody:[NONE (encoded body omitted)] ");
		} else {
			Buffer buffer = new Buffer();
			requestBody.writeTo(buffer);

			Charset charset = UTF8;
			okhttp3.MediaType contentType = requestBody.contentType();
			if (contentType != null) {
				charset = contentType.charset(UTF8);
			}

			if (isPlaintext(buffer)) {
				logStringBuider.append("requestbody:["+buffer.readString(charset)+"] ");


			} else {
				logStringBuider.append("requestbody:[length : " +requestBody.contentLength() +"--byte body omitted ] ");
			}
		}

		long startNs = System.nanoTime();
		Response response;
		try {
			response = chain.proceed(request);
		} catch (Exception e) {
			logStringBuider.append("response:[HTTPFAILED ,"+ExceptionUtils.getFullStackTrace(e)+"] ");
			logger.info(logStringBuider.toString());
			throw e;
		}
		long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

		ResponseBody responseBody = response.body();
		long contentLength = responseBody.contentLength();
		logStringBuider.append("Response Summary[ " + response.code() + (response.message().isEmpty() ? "" : ' ' + response.message()) + ' ' + response.request().url() + " (took " + tookMs + "ms )");

		headers = response.headers();
		for (int i = 0, count = headers.size(); i < count; i++) {
			logStringBuider.append("responseheader :["+headers.name(i) + ": " + headers.value(i)+"] ");
		}

		if (!HttpHeaders.hasBody(response)) {
			logStringBuider.append("responsebody:[ NONE ] ");
		} else if (bodyHasUnknownEncoding(response.headers())) {
			logStringBuider.append("responsebody:[NONE (encoded body omitted)] ");
		} else {
			BufferedSource source = responseBody.source();
			source.request(Long.MAX_VALUE); // Buffer the entire body.
			Buffer buffer = source.buffer();

			Long gzippedLength = null;
			if ("gzip".equalsIgnoreCase(headers.get("Content-Encoding"))) {
				gzippedLength = buffer.size();
				GzipSource gzippedResponseBody = null;
				try {
					gzippedResponseBody = new GzipSource(buffer.clone());
					buffer = new Buffer();
					buffer.writeAll(gzippedResponseBody);
				} finally {
					if (gzippedResponseBody != null) {
						gzippedResponseBody.close();
					}
					if (buffer != null) {
						buffer.close();
					}
				}
			}

			Charset charset = UTF8;
			okhttp3.MediaType contentType = responseBody.contentType();
			if (contentType != null) {
				charset = contentType.charset(UTF8);
			}

			if (!isPlaintext(buffer)) {
				logStringBuider.append("responsebody:[length : " +buffer.size() +"--byte body omitted ] ");

				return response;
			}

			if (contentLength != 0) {
				logStringBuider.append("responsebody:["+buffer.clone().readString(charset)+" ] ");
			}

			if (gzippedLength != null) {
				logStringBuider.append("responsebody:[" + buffer.size() + "-byte, " + gzippedLength + "-gzipped-byte body]");
			}
		}

		logger.info(logStringBuider.toString());
		
		return response;
	}
}