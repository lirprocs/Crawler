package org.example.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.JsonData;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.example.model.NewsDocument;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

public class ElasticsearchService {
    private static final String INDEX_NAME = "news";
    private final ElasticsearchClient client;

    public ElasticsearchService() {
        RestClient restClient = RestClient.builder(
                new HttpHost("localhost", 9200)
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        this.client = new ElasticsearchClient(transport);
        createIndexIfNotExists();
    }

    private void createIndexIfNotExists() {
        try {
            boolean exists = client.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (!exists) {
                client.indices().create(c -> c
                        .index(INDEX_NAME)
                        .mappings(m -> m
                                .properties("title", p -> p.text(t -> t))
                                .properties("publicationDate", p -> p.date(d -> d))
                                .properties("content", p -> p.text(t -> t))
                                .properties("url", p -> p.keyword(k -> k))
                                .properties("author", p -> p.keyword(k -> k))
                        )
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create index", e);
        }
    }

    public String generateId(NewsDocument document) {
        String input = document.getTitle() + document.getPublicationDate().toString();
        return String.valueOf(input.hashCode());
    }

    public boolean documentExists(String id) {
        try {
            GetResponse<NewsDocument> response = client.get(g -> g
                    .index(INDEX_NAME)
                    .id(id)
                    .sourceIncludes("id"), NewsDocument.class
            );
            return response.found();
        } catch (IOException e) {
            throw new RuntimeException("Failed to check document existence", e);
        }
    }

    public void saveDocument(NewsDocument document) {
        try {
            String id = generateId(document);
            document.setId(id);

            IndexResponse response = client.index(i -> i
                    .index(INDEX_NAME)
                    .id(id)
                    .document(document)
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to save document", e);
        }
    }

    public List<NewsDocument> search(String query, String author, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Query mainQuery = MultiMatchQuery.of(m -> m
                    .fields("title", "content")
                    .query(query)
            )._toQuery();

            Query dateRangeQuery = RangeQuery.of(r -> r
                    .field("publicationDate")
                    .gte(JsonData.of(startDate.format(DateTimeFormatter.ISO_DATE_TIME)))
                    .lte(JsonData.of(endDate.format(DateTimeFormatter.ISO_DATE_TIME)))
            )._toQuery();

            BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                    .must(mainQuery)
                    .must(dateRangeQuery);

            if (author != null && !author.isEmpty()) {
                Query authorQuery = TermQuery.of(t -> t
                        .field("author")
                        .value(author)
                )._toQuery();
                boolQuery.must(authorQuery);
            }

            SearchResponse<NewsDocument> response = client.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.bool(boolQuery.build()))
                    .size(100), NewsDocument.class
            );

            List<NewsDocument> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            System.out.println("\nНайдено новостей: " + results.size());
            System.out.println("----------------------------------------");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm, d MMMM yyyy");

            for (NewsDocument doc : results) {
                System.out.println("Заголовок: " + doc.getTitle());
                System.out.println("Автор: " + doc.getAuthor());
                System.out.println("Дата: " + doc.getPublicationDate().format(dateFormatter));
                System.out.println("URL: " + doc.getUrl());
                System.out.println("----------------------------------------");
            }

            return results;
        } catch (IOException e) {
            throw new RuntimeException("Failed to search documents", e);
        }
    }

    public List<NewsDocument> searchForDate(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Query dateRangeQuery = RangeQuery.of(r -> r
                    .field("publicationDate")
                    .gte(JsonData.of(startDate.format(DateTimeFormatter.ISO_DATE_TIME)))
                    .lte(JsonData.of(endDate.format(DateTimeFormatter.ISO_DATE_TIME)))
            )._toQuery();

            BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                    .must(dateRangeQuery);

            SearchResponse<NewsDocument> response = client.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.bool(boolQuery.build()))
                    .size(100), NewsDocument.class
            );

            List<NewsDocument> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            System.out.println("\nНайдено новостей: " + results.size());
            System.out.println("----------------------------------------");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm, d MMMM yyyy");

            for (NewsDocument doc : results) {
                System.out.println("Заголовок: " + doc.getTitle());
                System.out.println("Автор: " + doc.getAuthor());
                System.out.println("Дата: " + doc.getPublicationDate().format(dateFormatter));
                System.out.println("URL: " + doc.getUrl());
                System.out.println("----------------------------------------");
            }

            return results;
        } catch (IOException e) {
            throw new RuntimeException("Failed to search documents", e);
        }
    }

    public void getPublicationHistogram() {
        try {
            DateHistogramAggregation dateHistogram = DateHistogramAggregation.of(d -> d
                    .field("publicationDate")
                    .calendarInterval(CalendarInterval.Day)
                    .format("yyyy-MM-dd")
            );

            SearchResponse<Void> response = client.search(s -> s
                    .index(INDEX_NAME)
                    .size(0)
                    .aggregations("publications_by_date", a -> a
                            .dateHistogram(dateHistogram)
                    ), Void.class
            );

            System.out.println("\nСтатистика публикаций по датам:");
            System.out.println("----------------------------------------");
            response.aggregations().get("publications_by_date").dateHistogram().buckets().array()
                    .forEach(bucket -> {
                        String date = bucket.keyAsString().substring(0, 10);
                        System.out.println("Дата: " + date +
                                ", Количество публикаций: " + bucket.docCount());
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to get histogram", e);
        }
    }

    public List<NewsDocument> advancedSearch(String query, String author, LocalDateTime startDate, LocalDateTime endDate, 
                                           String[] fields, String[] operators) {
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            Query fullTextQuery = QueryStringQuery.of(q -> q
                    .query(query)
                    .fields(Arrays.asList(fields))
                    .defaultOperator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
            )._toQuery();
            boolQuery.must(fullTextQuery);

            if (startDate != null && endDate != null) {
                Query dateRangeQuery = RangeQuery.of(r -> r
                        .field("publicationDate")
                        .gte(JsonData.of(startDate.format(DateTimeFormatter.ISO_DATE_TIME)))
                        .lte(JsonData.of(endDate.format(DateTimeFormatter.ISO_DATE_TIME)))
                )._toQuery();
                boolQuery.must(dateRangeQuery);
            }

            if (author != null && !author.isEmpty()) {
                Query authorQuery = TermQuery.of(t -> t
                        .field("author")
                        .value(author)
                )._toQuery();
                boolQuery.must(authorQuery);
            }

            SearchResponse<NewsDocument> response = client.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.bool(boolQuery.build()))
                    .size(100), NewsDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to perform advanced search", e);
        }
    }

    public void getAuthorPublicationHistogram() {
        try {
            SearchResponse<Void> response = client.search(s -> s
                    .index(INDEX_NAME)
                    .size(0)
                    .aggregations("publications_by_author", a -> a
                            .terms(t -> t.field("author").size(20))
                    ), Void.class
            );

            System.out.println("\nСтатистика публикаций по авторам:");
            System.out.println("----------------------------------------");
            response.aggregations().get("publications_by_author").sterms().buckets().array()
                    .forEach(bucket -> {
                        String author = bucket.key().stringValue();
                        System.out.println("Автор: '" + author + "'" +
                                ", Количество публикаций: " + bucket.docCount());
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to get author histogram", e);
        }
    }

    public void getDateAndAuthorHistogram() {
        try {
            SearchResponse<Void> response = client.search(s -> s
                    .index(INDEX_NAME)
                    .size(0)
                    .aggregations("publications_by_date", a -> a
                            .dateHistogram(d -> d
                                    .field("publicationDate")
                                    .calendarInterval(CalendarInterval.Day)
                                    .format("yyyy-MM-dd")
                            )
                            .aggregations("by_author", a2 -> a2
                                    .terms(t -> t.field("author").size(5))
                            )
                    ), Void.class
            );

            System.out.println("\nСтатистика публикаций по датам и авторам:");
            System.out.println("----------------------------------------");
            response.aggregations().get("publications_by_date").dateHistogram().buckets().array()
                    .forEach(dateBucket -> {
                        String date = dateBucket.keyAsString().substring(0, 10);
                        System.out.println("\nДата: " + date);
                        dateBucket.aggregations().get("by_author").sterms().buckets().array()
                                .forEach(authorBucket -> {
                                    System.out.println("  Автор: " + authorBucket.key().stringValue() +
                                            ", Публикаций: " + authorBucket.docCount());
                                });
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to get date and author histogram", e);
        }
    }
} 