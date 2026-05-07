package com.yoneodoo.api.controller;

import com.yoneodoo.api.dto.FridgeAddRequest;
import com.yoneodoo.api.service.UserFridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 "내 냉장고" API 진입점입니다.
 * <p>
 * <b>저장 구조</b><br>
 * 별도 냉장고 테이블이 아니라 {@code users.fridge_ingredients} jsonb 컬럼(문자열 배열)을
 * {@link UserFridgeService}가 읽고/수정합니다.
 * <p>
 * <b>임시 설계 참고</b><br>
 * 현재는 요청 본문에 {@code userId}를 직접 받습니다. 로그인 세션이 붙으면 토큰에서 유저를 식별하도록 바꾸는 것이 일반적입니다.
 */
@RestController
@RequestMapping("/api/v1/fridge")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UserFridgeController {

    private final UserFridgeService userFridgeService;

    /** 재료 이름 목록을 유저 냉장고에 추가합니다(중복은 서비스에서 무시). */
    @PostMapping("/add")
    public String addIngredients(@RequestBody FridgeAddRequest request) {
        userFridgeService.addIngredientsToFridge(request);
        return "냉장고에 재료가 성공적으로 추가되었습니다!";
    }

    /**
     * 해당 유저의 냉장고 재료 이름 배열을 반환합니다.
     * 형식 예: {@code ["계란","고추장"]}.
     */
    @GetMapping("/{userId}")
    public List<String> getMyFridge(@PathVariable(name = "userId") Long userId) {
        return userFridgeService.getMyFridgeIngredients(userId);
    }

    /**
     * 냉장고에서 재료 한 개를 삭제합니다.
     * <p>
     * 경로의 {@code ingredientName}은 URL 인코딩된 문자열로 전달되는 경우가 많습니다.
     * 서버는 스프링이 디코딩한 문자열을 서비스로 넘깁니다.
     */
    @DeleteMapping("/{userId}/ingredients/{ingredientName}")
    public String removeIngredient(
            @PathVariable(name = "userId") Long userId,
            @PathVariable(name = "ingredientName") String ingredientName) {

        userFridgeService.removeIngredientFromFridge(userId, ingredientName);
        return "냉장고에서 [" + ingredientName + "] 재료가 성공적으로 삭제되었습니다!";
    }
}
