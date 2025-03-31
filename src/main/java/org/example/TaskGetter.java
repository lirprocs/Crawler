package org.example;

import com.rabbitmq.client.*;
import connection.RabbitMQConnector;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class TaskGetter {
    private static final String TASK_QUEUE = "task_queue";
    
    public static void getTask() {
        try (Connection connection = RabbitMQConnector.getConnection();
             Channel channel = RabbitMQConnector.createChannel(connection)) {
            
            GetResponse response = channel.basicGet(TASK_QUEUE, false);
            if (response != null) {
                String url = new String(response.getBody(), "UTF-8");
                System.out.println(" [x] Получено через basicGet: " + url);
                channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
            } else {
                System.out.println(" [x] Нет доступных сообщений в очереди");
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
} 