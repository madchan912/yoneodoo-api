package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // String 타입에서 Enum 타입으로 변경! (DB에는 문자열로 저장되도록 STRING 옵션 추가)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderType provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(nullable = false, length = 50)
    private String nickname;

    public User(ProviderType provider, String providerId, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
    }
}