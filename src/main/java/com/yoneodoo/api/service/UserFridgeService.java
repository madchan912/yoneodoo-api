package com.yoneodoo.api.service;

import com.yoneodoo.api.dto.FridgeAddRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFridgeService {

    // 필요한 Repository들을 주입받습니다. (이름은 개발자님 환경에 맞게!)
    // private final UserRepository userRepository;
    // private final IngredientRepository ingredientRepository;
    // private final UserIngredientRepository userIngredientRepository;

    @Transactional
    public void addIngredientsToFridge(FridgeAddRequest request) {
        // 1. 유저가 진짜 있는지 확인 (나중엔 예외 처리 추가)
        // User user = userRepository.findById(request.getUserId()).orElseThrow();

        // 2. 넘어온 재료 ID 목록(1, 2, 4)으로 DB에서 재료들을 싹 다 조회
        // List<Ingredient> ingredients = ingredientRepository.findAllById(request.getIngredientIds());

        // 3. 유저의 냉장고(UserIngredient)에 하나씩 예쁘게 포장해서 저장
        /*
        for (Ingredient ingredient : ingredients) {
            UserIngredient userIngredient = UserIngredient.builder()
                    .user(user)
                    .ingredient(ingredient)
                    .build();
            userIngredientRepository.save(userIngredient);
        }
        */

        System.out.println("✅ " + request.getUserId() + "번 유저의 냉장고에 재료 " + request.getIngredientIds().size() + "개 저장 완료!");
    }
}