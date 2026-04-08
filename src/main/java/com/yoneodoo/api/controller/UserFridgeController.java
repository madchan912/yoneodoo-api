package com.yoneodoo.api.controller;

import com.yoneodoo.api.dto.FridgeAddRequest;
import com.yoneodoo.api.service.UserFridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fridge")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // 🚀 리액트(Vite) 접근 허용!
public class UserFridgeController {

    private final UserFridgeService userFridgeService;

    // 1. 냉장고에 재료 추가
    @PostMapping("/add")
    public String addIngredients(@RequestBody FridgeAddRequest request) {
        userFridgeService.addIngredientsToFridge(request);
        return "냉장고에 재료가 성공적으로 추가되었습니다!";
    }

    // 2. 내 냉장고 조회 API (GET)
    // 🚀 이제 복잡한 DTO 대신 단순한 String 리스트(["계란", "고추장"])를 반환합니다.
    @GetMapping("/{userId}")
    public List<String> getMyFridge(@PathVariable(name = "userId") Long userId) {
        return userFridgeService.getMyFridgeIngredients(userId);
    }

    // 3. 내 냉장고 재료 삭제 API (DELETE)
    // 🚀 URL 경로에 재료 ID(숫자) 대신 한글 이름(ingredientName)이 들어옵니다.
    @DeleteMapping("/{userId}/ingredients/{ingredientName}")
    public String removeIngredient(
            @PathVariable(name = "userId") Long userId,
            @PathVariable(name = "ingredientName") String ingredientName) {

        userFridgeService.removeIngredientFromFridge(userId, ingredientName);
        return "냉장고에서 [" + ingredientName + "] 재료가 성공적으로 삭제되었습니다!";
    }
}