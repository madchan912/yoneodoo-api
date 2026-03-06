package com.yoneodoo.api.controller;

import com.yoneodoo.api.dto.FridgeAddRequest;
import com.yoneodoo.api.dto.FridgeIngredientResponse;
import com.yoneodoo.api.service.UserFridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fridge")
@RequiredArgsConstructor
public class UserFridgeController {

    private final UserFridgeService userFridgeService;

    // 데이터를 숨겨서 받는 POST 방식! (@RequestBody 사용)
    @PostMapping("/add")
    public String addIngredients(@RequestBody FridgeAddRequest request) {

        userFridgeService.addIngredientsToFridge(request);

        return "냉장고에 재료가 성공적으로 추가되었습니다!";
    }

    // 내 냉장고 조회 API (GET)
    @GetMapping("/{userId}")
    public List<FridgeIngredientResponse> getMyFridge(@PathVariable(name = "userId") Long userId) {

        // 서비스의 조회 로직을 호출해서 바로 리턴!
        return userFridgeService.getMyFridgeIngredients(userId);
    }

    // 내 냉장고 재료 삭제 API (DELETE)
    @DeleteMapping("/{userId}/ingredients/{ingredientId}")
    public String removeIngredient(
            @PathVariable(name = "userId") Long userId,
            @PathVariable(name = "ingredientId") Long ingredientId) {

        userFridgeService.removeIngredientFromFridge(userId, ingredientId);

        return "냉장고에서 재료가 성공적으로 삭제되었습니다!";
    }
}