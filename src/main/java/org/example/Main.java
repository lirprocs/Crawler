package org.example;

import org.example.service.ElasticsearchService;
import org.example.model.NewsDocument;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ElasticsearchService elasticsearchService = new ElasticsearchService();

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
            ResultConsumer.startConsuming(elasticsearchService);
        }).start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nCrawler by Lirprocs");
            System.out.println("Выберите действие:");
            System.out.println("1. Поиск новостей");
            System.out.println("2. Просмотр статистики публикаций");
            System.out.println("3. Вывод всего");
            System.out.println("4. Расширенный поиск");
            System.out.println("5. Статистика по авторам");
            System.out.println("6. Комбинированная статистика");
            System.out.println("7. Выход");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.println("Введите поисковый запрос:");
                    String query = scanner.nextLine();
                    System.out.println("Введите автора (или оставьте пустым):");
                    String author = scanner.nextLine();
                    System.out.println("Введите начальную дату (yyyy-MM-dd):");
                    String startDate = scanner.nextLine();
                    System.out.println("Введите конечную дату (yyyy-MM-dd):");
                    String endDate = scanner.nextLine();

                    LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
                    LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");

                   elasticsearchService.search(query, author, start, end);
                    break;

                case 2:
                    System.out.println("Статистика публикаций:");
                    elasticsearchService.getPublicationHistogram();
                    break;

                case 3:
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime startDate1 = now.minusDays(30);
                    elasticsearchService.searchForDate(startDate1, now);
                    break;

                case 4:
                    System.out.println("Введите поисковый запрос (можно использовать синтаксис Elasticsearch):");
                    String advancedQuery = scanner.nextLine();

                    List<NewsDocument> advancedResults = elasticsearchService.advancedSearch(
                            advancedQuery, null, null, null, new String[]{"title", "content", "author"}, null
                    );

                    System.out.println("\nНайдено новостей: " + advancedResults.size());
                    System.out.println("----------------------------------------");

                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm, d MMMM yyyy");
                    for (NewsDocument doc : advancedResults) {
                        System.out.println("Заголовок: " + doc.getTitle());
                        System.out.println("Автор: " + doc.getAuthor());
                        System.out.println("Дата: " + doc.getPublicationDate().format(dateFormatter));
                        System.out.println("URL: " + doc.getUrl());
                        System.out.println("----------------------------------------");
                    }
                    break;

                case 5:
                    elasticsearchService.getAuthorPublicationHistogram();
                    break;

                case 6:
                    elasticsearchService.getDateAndAuthorHistogram();
                    break;

                case 7:
                    System.out.println("Выход...");
                    System.exit(0);
                    break;

                default:
                    System.out.println("Неверный выбор");
            }
        }
    }
}
