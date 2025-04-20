package org.example;

import com.rabbitmq.client.*;
import connection.RabbitMQConnector;
import org.example.model.NewsDocument;
import org.example.service.ElasticsearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ResultConsumer {
    private static final String RESULT_QUEUE = "result_queue";
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Logger LOGGER = LogManager.getLogger(ResultConsumer.class);

    public static void startConsuming(ElasticsearchService elasticsearchService) {
        try {
            Connection connection = RabbitMQConnector.getConnection();
            Channel channel = RabbitMQConnector.createChannel(connection);

            channel.basicConsume(RESULT_QUEUE, true, (consumerTag, delivery) -> {
                try {
                    String message = new String(delivery.getBody(), "UTF-8");
                    NewsDocument document = objectMapper.readValue(message, NewsDocument.class);
                    
                    if (document != null) {
                        String id = elasticsearchService.generateId(document);
                        if (!elasticsearchService.documentExists(id)) {
                            elasticsearchService.saveDocument(document);
                            LOGGER.info("Сохранен документ: {}", document.getTitle());
                        } else {
                            LOGGER.info("Документ уже существует: {}", document.getTitle());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Ошибка при обработке сообщения: {}", e.getMessage());
                }
            }, consumerTag -> {});

        } catch (IOException | TimeoutException e) {
            LOGGER.error("Ошибка при подключении к RabbitMQ: {}", e.getMessage());
        }
    }
}
