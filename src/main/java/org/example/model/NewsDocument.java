package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;

public class NewsDocument {
    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("publicationDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime publicationDate;

    @JsonProperty("content")
    private String content;

    @JsonProperty("url")
    private String url;

    @JsonProperty("author")
    private String author;

    @JsonCreator
    public NewsDocument() {
    }

    @JsonCreator
    public NewsDocument(
            @JsonProperty("title") String title,
            @JsonProperty("publicationDate") LocalDateTime publicationDate,
            @JsonProperty("content") String content,
            @JsonProperty("url") String url,
            @JsonProperty("author") String author) {
        this.title = title;
        this.publicationDate = publicationDate;
        this.content = content;
        this.url = url;
        this.author = author;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getPublicationDate() {
        return publicationDate;
    }

    public String getUrl() {
        return url;
    }

    public String getAuthor() {
        return author;
    }

}

