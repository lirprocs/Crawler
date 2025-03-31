package org.example;

public class Main {
    public static void main(String[] args) {
        TaskQueue.setupQueues();

        new Thread(() -> {
            System.out.println("Запускаем Crawler...");
            Crawler.startCrawling();
        }).start();

        new Thread(() -> {
            System.out.println("Запускаем TaskConsumer...");
            TaskConsumer.startConsuming();
        }).start();

        new Thread(() -> {
            System.out.println("Запускаем ResultConsumer...");
            ResultConsumer.main(new String[]{});
        }).start();

        // Демонстрация использования basicGet
        new Thread(() -> {
            System.out.println("Демонстрация basicGet...");
            TaskGetter.getTask();
        }).start();
    }
}
