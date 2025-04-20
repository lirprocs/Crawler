package org.example;

import com.rabbitmq.client.*;
import connection.RabbitMQConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.example.model.NewsDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeoutException;

public class TaskConsumer {
    private static final String TASK_QUEUE = "task_queue";
    private static final String RESULT_QUEUE = "result_queue";
    private static final Logger LOGGER = LogManager.getLogger(TaskConsumer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final DateTimeFormatter inputDateFormatter = DateTimeFormatter.ofPattern("HH:mm, d MMMM yyyy");
    private static final DateTimeFormatter outputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static void startConsuming() {
        try {
            Connection connection = RabbitMQConnector.getConnection();
            Channel channel = RabbitMQConnector.createChannel(connection);

            channel.basicConsume(TASK_QUEUE, false, (consumerTag, delivery) -> {
                String url = new String(delivery.getBody(), "UTF-8");
                LOGGER.info("Обрабатываем: {}", url);

                try {
                    String normalizedUrl = Crawler.normalizeUrl(url);
                    if (normalizedUrl != null) {
                        NewsDocument document = processPage(normalizedUrl);
                        if (document != null) {
                            String json = objectMapper.writeValueAsString(document);
                            channel.basicPublish("", RESULT_QUEUE, null, json.getBytes());
                            LOGGER.info("Сохранено в result_queue");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Ошибка при обработке URL {}: {}", url, e.getMessage());
                }

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }, consumerTag -> {});

        } catch (IOException | TimeoutException e) {
            LOGGER.error("Ошибка при подключении к RabbitMQ: {}", e.getMessage());
        }
    }


    private static NewsDocument processPage(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            
            String title = doc.select(".topic-body__title").text();
            String dateStr = doc.select(".topic-header__item.topic-header__time").text();
            String author = doc.select(".topic-authors__name").text();
            String content = doc.select(".topic-body__content-text").text();

            if (title.isEmpty() || dateStr.isEmpty() || author.isEmpty() || content.isEmpty()) {
                LOGGER.warn("Неполные данные на странице: {}", url);
                return null;
            }

            LocalDateTime date = LocalDateTime.parse(dateStr, inputDateFormatter);
            return new NewsDocument(title, date, content, url, author);
        } catch (Exception e) {
            LOGGER.error("Ошибка при обработке страницы {}: {}", url, e.getMessage());
            return null;
        }
    }
}
