package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_fridges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFridge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    public UserFridge(User user, Ingredient ingredient) {
        this.user = user;
        this.ingredient = ingredient;
    }
}