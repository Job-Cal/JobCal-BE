package kr.co.jobcal.global.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpFetcher {

    private static final Logger logger = LoggerFactory.getLogger(HttpFetcher.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public static String fetchUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            logger.warn("Failed to fetch URL {} with status {}", url, response.statusCode());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Error fetching URL {}: {}", url, e.getMessage());
            return null;
        } catch (IOException e) {
            logger.warn("Error fetching URL {}: {}", url, e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid URL {}: {}", url, e.getMessage());
            return null;
        }
    }
}
