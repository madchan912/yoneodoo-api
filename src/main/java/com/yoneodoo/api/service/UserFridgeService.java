package com.yoneodoo.api.service;

import com.yoneodoo.api.dto.FridgeAddRequest;
import com.yoneodoo.api.entity.User;
import com.yoneodoo.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFridgeService {

    // 🚀 이제 잡다한 Repository는 안 쓰고 오직 UserRepository만 씁니다!
    private final UserRepository userRepository;

    // 1. 냉장고에 재료 추가
    @Transactional
    public void addIngredientsToFridge(FridgeAddRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 냉장고가 비어있으면 초기화
        if (user.getFridgeIngredients() == null) {
            user.setFridgeIngredients(new ArrayList<>());
        }

        // 기존 냉장고에 없는 재료만 쏙쏙 골라서 추가 (중복 방지)
        for (String newIngredient : request.getIngredients()) {
            if (!user.getFridgeIngredients().contains(newIngredient)) {
                user.getFridgeIngredients().add(newIngredient);
            }
        }

        userRepository.save(user); // JPA가 알아서 JSONB로 업데이트해 줍니다!
        System.out.println("✅ " + request.getUserId() + "번 유저의 냉장고 업데이트 완료!");
    }

    // 2. 내 냉장고 재료 조회
    @Transactional(readOnly = true)
    public List<String> getMyFridgeIngredients(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 🚀 DTO 변환 없이 그냥 문자열 리스트(["계란", "고추장"]) 바로 리턴!
        return user.getFridgeIngredients() != null ? user.getFridgeIngredients() : new ArrayList<>();
    }

    // 3. 내 냉장고 재료 삭제
    @Transactional
    public void removeIngredientFromFridge(Long userId, String ingredientName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        if (user.getFridgeIngredients() != null) {
            // 리스트에서 해당 이름 쏙 빼고 저장
            user.getFridgeIngredients().remove(ingredientName);
            userRepository.save(user);
            System.out.println("✅ " + userId + "번 유저의 냉장고에서 [" + ingredientName + "] 삭제 완료!");
        }
    }
}