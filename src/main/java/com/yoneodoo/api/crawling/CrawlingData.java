package com.yoneodoo.api.crawling;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawling_data")
@Getter
@NoArgsConstructor
public class CrawlingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}