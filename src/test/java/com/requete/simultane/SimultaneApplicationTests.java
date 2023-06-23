package com.requete.simultane;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.net.http.HttpClient.newHttpClient;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class SimultaneApplicationTests {

	public static final String AUTHORIZATION_HEADER = "Authorization";

	@Test
	public void multipleCall() {
		int requestsNb = 100;
		CountDownLatch latch = new CountDownLatch(1);
		ExecutorService executorService = Executors.newFixedThreadPool(requestsNb);

		var futures = new ArrayList<Future<String>>();
		for (var c = 0; c < requestsNb; c++) {
			futures.add(
					executorService.submit(() -> singleGetRequest(
							new URI("https://gy64x79zu7.execute-api.eu-west-3.amazonaws.com/Prod/addition?a=1&b=9"),
							"",
							latch)));
		}
		latch.countDown();

		List<String> retrieved = futures.stream()
				.map(this::getOptionalFutureResult)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.peek(taratasyList -> assertEquals("{\"result\":10}", taratasyList))
				.toList();
		assertEquals(retrieved.size(), requestsNb);

	}

	public String singleGetRequest(URI uri, String bearer, CountDownLatch latch) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.GET()
				.header(AUTHORIZATION_HEADER, bearer)
				.build();
		try {
			latch.await();
			HttpResponse<String> response = newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			return response.body();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}


	@SneakyThrows
	public <T> Optional<T> getOptionalFutureResult(Future<T> future) {
		try {
			return Optional.of(future.get());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
