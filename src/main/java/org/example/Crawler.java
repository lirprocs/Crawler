package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler {
    private static final String BASE_URL = "https://lenta.ru/parts/news/";
    private static final Logger LOGGER = LogManager.getLogger(Crawler.class);
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Optional<Document> doc = getPage(BASE_URL);
        doc.ifPresent(Crawler::processDocument);
    }

    private static Optional<Document> getPage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                return Optional.of(Jsoup.parse(response.body()));
            } else {
                handleHttpError(statusCode, url);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Ошибка загрузки страницы {}: {}", url, e.getMessage());
        }
        return Optional.empty();
    }

    private static void processDocument(Document doc) {
        System.out.println("Страница получена:");
        Elements newsElements = doc.select(".parts-page__item");
        for (Element news : newsElements) {
            String title = news.select(".card-full-news__title").text();
            String link = "https://lenta.ru" + news.select("._parts-news").attr("href");
            String time = news.select(".card-full-news__date").text();
            String topic = news.select(".card-full-news__rubric").text();

            System.out.println("Title: " + title);
            System.out.println("Time: " + time);
            System.out.println("Topic: " + topic);
            System.out.println("Link: " + link);
            System.out.println("--------------------------------------------");
        }
    }

    private static void handleHttpError(int statusCode, String url) {
        switch (statusCode) {
            case 400 -> LOGGER.error("Error 400 Bad Request for URL: {}", url);
            case 403 -> LOGGER.error("Error 403 Forbidden for URL: {}", url);
            case 404 -> LOGGER.error("Error 404 Not Found for URL: {}", url);
            case 500 -> LOGGER.error("Error 500 Internal Server Error for URL: {}", url);
            default -> LOGGER.error("Unexpected HTTP status code {} for URL: {}", statusCode, url);
        }
    }
}
