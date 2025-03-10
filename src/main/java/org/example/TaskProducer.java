package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import connection.RabbitMQConnector;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class TaskProducer {
    private static final String TASK_QUEUE = "task_queue";

    public static void sendTask(String url) {
        try (Connection connection = RabbitMQConnector.getConnection();
             Channel channel = RabbitMQConnector.createChannel(connection)) {

            channel.basicPublish("", TASK_QUEUE, null, url.getBytes());

            System.out.println(" [x] Отправлено в очередь: " + url);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
