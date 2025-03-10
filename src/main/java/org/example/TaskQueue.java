package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import connection.RabbitMQConnector;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class TaskQueue {
    private static final String TASK_QUEUE = "task_queue";
    private static final String RESULT_QUEUE = "result_queue";

    public static void setupQueues() {
        try (Connection connection = RabbitMQConnector.getConnection();
             Channel channel = RabbitMQConnector.createChannel(connection)) {

            channel.queueDeclare(TASK_QUEUE, true, false, false, null);

            channel.queueDeclare(RESULT_QUEUE, true, false, false, null);

            System.out.println("Очереди созданы: " + TASK_QUEUE + " и " + RESULT_QUEUE);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
