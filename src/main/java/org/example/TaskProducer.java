package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import connection.RabbitMQConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

public class TaskProducer {
    private static final String TASK_QUEUE = "task_queue";
    private static final Logger LOGGER = LogManager.getLogger(TaskProducer.class);

    public static void sendTask(String url) {
        try {
            String normalizedUrl = Crawler.normalizeUrl(url);
            if (normalizedUrl == null) {
                return;
            }

        try (Connection connection = RabbitMQConnector.getConnection();
             Channel channel = RabbitMQConnector.createChannel(connection)) {

                channel.basicPublish("", TASK_QUEUE, null, normalizedUrl.getBytes());
                LOGGER.info("Отправлено в очередь: {}", normalizedUrl);
            }
        } catch (IOException | TimeoutException e) {
            LOGGER.error("Ошибка при отправке URL {}: {}", url, e.getMessage());
        }
    }
}
