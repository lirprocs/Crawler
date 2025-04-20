package org.example;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

public class Crawler {
    private static final String BASE_URL = "https://lenta.ru/parts/news/";
    private static final Logger LOGGER = LogManager.getLogger(Crawler.class);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private static final int SOCKET_TIMEOUT_MS = 10000;

    private static final CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(Timeout.of(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                    .setResponseTimeout(Timeout.of(SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                    .build())
            .build();

    public static void startCrawling() {
        Document doc = getPageWithRetry(BASE_URL);
        if (doc != null) {
            Elements newsElements = doc.select(".parts-page__item");
            for (Element news : newsElements) {
                String href = news.select("._parts-news").attr("href");
                String normalizedUrl = normalizeUrl(href);
                if (normalizedUrl != null) {
                    TaskProducer.sendTask(normalizedUrl);
                }
            }
        }
    }

    public static String normalizeUrl(String url) {
        try {
            if (url.startsWith("https://lenta.ruhttps://")) {
                url = url.substring("https://lenta.ru".length());
            }

            if (!url.startsWith("http")) {
                url = "https://lenta.ru" + (url.startsWith("/") ? url : "/" + url);
            }

            URI uri = new URI(url);
            if (!uri.getHost().endsWith("lenta.ru")) {
                LOGGER.warn("Пропущен URL с неподдерживаемым доменом: {}", url);
                return null;
            }

            if (uri.getPath() == null || uri.getPath().isEmpty() || uri.getPath().equals("/")) {
                LOGGER.warn("Пропущен URL без пути: {}", url);
                return null;
            }

            return url;
        } catch (URISyntaxException e) {
            LOGGER.error("Некорректный URL: {}", url);
            return null;
        }
    }

    private static Document getPageWithRetry(String url) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Document doc = getPage(url);
                if (doc != null) {
                    return doc;
                }
                if (attempt < MAX_RETRIES) {
                    LOGGER.warn("Попытка {} из {} не удалась, повтор через {} мс",
                            attempt, MAX_RETRIES, RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Прервано ожидание между попытками: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    private static Document getPage(String url) {
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        request.setHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");

        try (ClassicHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();

            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String html = EntityUtils.toString(entity);
                    return Jsoup.parse(html);
                }
            } else {
                handleHttpError(statusCode, url);
            }
        } catch (IOException | ParseException e) {
            LOGGER.error("Ошибка при загрузке страницы {}: {}", url, e.getMessage());
        }
        return null;
    }

    private static void handleHttpError(int statusCode, String url) {
        String errorMessage;
        switch (statusCode) {
            case HttpStatus.SC_BAD_REQUEST:
                errorMessage = "Некорректный запрос";
                break;
            case HttpStatus.SC_UNAUTHORIZED:
                errorMessage = "Требуется авторизация";
                break;
            case HttpStatus.SC_FORBIDDEN:
                errorMessage = "Доступ запрещен";
                break;
            case HttpStatus.SC_NOT_FOUND:
                errorMessage = "Страница не найдена";
                break;
            case HttpStatus.SC_TOO_MANY_REQUESTS:
                errorMessage = "Слишком много запросов";
                break;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                errorMessage = "Внутренняя ошибка сервера";
                break;
            case HttpStatus.SC_SERVICE_UNAVAILABLE:
                errorMessage = "Сервис временно недоступен";
                break;
            case HttpStatus.SC_GATEWAY_TIMEOUT:
                errorMessage = "Таймаут шлюза";
                break;
            default:
                errorMessage = "Неожиданный код состояния";
                break;
        }
        LOGGER.error("HTTP ошибка {} ({}) для URL: {}", statusCode, errorMessage, url);
    }
}
