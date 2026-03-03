package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // 예: "간장", "돼지고기"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientType type; // MAIN or SUB

    public Ingredient(String name, IngredientType type) {
        this.name = name;
        this.type = type;
    }
}