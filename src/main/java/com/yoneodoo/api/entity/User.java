package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // 🚀 내용 수정을 위해 Setter 추가!
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter // 🚀 추가됨!
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderType provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(nullable = false, length = 50)
    private String nickname;

    // 🚀 [추가됨] 내 냉장고 재료들을 문자열 리스트로 심플하게 저장!
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fridge_ingredients", columnDefinition = "jsonb")
    private List<String> fridgeIngredients = new ArrayList<>();

    public User(ProviderType provider, String providerId, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
    }
}