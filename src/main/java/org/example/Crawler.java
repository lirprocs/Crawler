package org.example;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Crawler {
    private static final String BASE_URL = "https://lenta.ru/parts/news/";
    private static final Logger LOGGER = LogManager.getLogger(Crawler.class);

    public static void main(String[] args) {
        Document doc = getPage(BASE_URL);
        if (doc != null) {
            System.out.println("Страница получена:");
            Elements newsElements = doc.select(".parts-page__item");
            for (Element news : newsElements) {
                String title = news.select(".card-full-news__title").text();
                String link = "https://lenta.ru" + news.select("._parts-news").attr("href");
                String time = news.select(".card-full-news__date").text();
                String author = news.select(".card-full-news__rubric").text();

                System.out.println("Title: " + title);
                System.out.println("Time: " + time);
                System.out.println("Author: " + author);
                System.out.println("Link: " + link);
                System.out.println("--------------------------------------------");
            }
        }
    }

    private static Document getPage(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .timeout(10000)
                    .execute();

            int statusCode = response.statusCode();
            if (statusCode == 200) {
                return response.parse();
            } else {
                handleHttpError(statusCode, url);
                return null;
            }
        } catch (IOException e) {
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
