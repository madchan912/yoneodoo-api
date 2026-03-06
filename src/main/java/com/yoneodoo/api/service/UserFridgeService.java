package com.yoneodoo.api.service;

import com.yoneodoo.api.dto.FridgeAddRequest;
import com.yoneodoo.api.dto.FridgeIngredientResponse;
import com.yoneodoo.api.entity.UserFridge;
import com.yoneodoo.api.repository.UserFridgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserFridgeService {

    // 필요한 Repository들을 주입받습니다. (이름은 개발자님 환경에 맞게!)
    // private final UserRepository userRepository;
    // private final IngredientRepository ingredientRepository;
    // private final UserIngredientRepository userIngredientRepository;
    private final UserFridgeRepository userFridgeRepository;

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

    // 내 냉장고 재료 조회 로직 (GET)
    @Transactional(readOnly = true)
    public List<FridgeIngredientResponse> getMyFridgeIngredients(Long userId) {

        // 1. 유저 ID로 내 냉장고(UserFridge) 테이블을 싹 다 뒤져서 가져옵니다.
        List<UserFridge> myFridgeList = userFridgeRepository.findByUserId(userId);

        // 2. DB에서 꺼낸 엔티티들을 방금 만든 DTO 포장지로 예쁘게 변환해서 리턴합니다.
        return myFridgeList.stream()
                .map(fridge -> new FridgeIngredientResponse(
                        fridge.getIngredient().getId(),
                        fridge.getIngredient().getName(),
                        fridge.getIngredient().getType().name() // Enum을 String("MAIN", "SUB")으로 변환!
                ))
                .collect(Collectors.toList());
    }

    // 내 냉장고 재료 삭제 로직 (DELETE)
    @Transactional
    public void removeIngredientFromFridge(Long userId, Long ingredientId) {

        // Repository의 삭제 메서드를 호출해서 DB에서 싹 날려버립니다!
        userFridgeRepository.deleteByUserIdAndIngredientId(userId, ingredientId);

        System.out.println("✅ " + userId + "번 유저의 냉장고에서 " + ingredientId + "번 재료 삭제 완료!");
    }
}