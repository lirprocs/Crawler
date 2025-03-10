package org.example;

import com.rabbitmq.client.*;
import connection.RabbitMQConnector;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ResultConsumer {
    private static final String RESULT_QUEUE = "result_queue";

    //public static void startConsuming() {
    public static void main(String[] args){
        try {
            Connection connection = RabbitMQConnector.getConnection();
            Channel channel = RabbitMQConnector.createChannel(connection);

            channel.basicConsume(RESULT_QUEUE, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Получен результат: \n" + message);
                System.out.println("--------------------------------------------");
            }, consumerTag -> {});

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
