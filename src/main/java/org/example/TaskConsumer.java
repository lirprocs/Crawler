package org.example;

import com.rabbitmq.client.*;
import connection.RabbitMQConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeoutException;

public class TaskConsumer {
    private static final String TASK_QUEUE = "task_queue";
    private static final String RESULT_QUEUE = "result_queue";
    private static final Logger LOGGER = LogManager.getLogger(Crawler.class);
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void startConsuming() {
        try {
            Connection connection = RabbitMQConnector.getConnection();
            Channel channel = RabbitMQConnector.createChannel(connection);

            channel.basicConsume(TASK_QUEUE, false, (consumerTag, delivery) -> {
                String url = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Обрабатываем: " + url);

                String result = processPage(url);
                channel.basicPublish("", RESULT_QUEUE, null, result.getBytes());
                System.out.println(" [x] Сохранено в result_queue");

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }, consumerTag -> {});

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private static String processPage(String url) {
        Document doc = getPage(url);
        if (doc != null) {
            String title = doc.select(".topic-body__title").text();
            String date = doc.select(".topic-header__item.topic-header__time").text();
            String name = doc.select(".topic-authors__name").text();
            String content = doc.select(".topic-body__content-text").text();
            return String.format("Title: %s\nDate: %s\nName: %s\nContent: %s\nURL: %s", title, date, name, content, url);
        }
        return "Error: Could not process page " + url;
    }

    private static Document getPage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                return Jsoup.parse(response.body());
            } else {
                handleHttpError(statusCode, url);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Ошибка загрузки страницы {}: {}", url, e.getMessage());
            return null;
        }
    }

    private static void handleHttpError(int statusCode, String url) {
        switch (statusCode) {
            case 400:
                LOGGER.error("Error 400 Bad Request for URL: {}", url);
                break;
            case 403:
                LOGGER.error("Error 403 Forbidden for URL: {}", url);
                break;
            case 404:
                LOGGER.error("Error 404 Not Found for URL: {}", url);
                break;
            case 500:
                LOGGER.error("Error 500 Internal Server Error for URL: {}", url);
                break;
            default:
                LOGGER.error("Unexpected HTTP status code {} for URL: {}", statusCode, url);
                break;
        }
    }
}
