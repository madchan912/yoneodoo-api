package com.yoneodoo.api.controller;

import com.yoneodoo.api.dto.FridgeAddRequest;
import com.yoneodoo.api.service.UserFridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}