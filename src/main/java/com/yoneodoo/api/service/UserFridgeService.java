package com.yoneodoo.api.service;

import com.yoneodoo.api.dto.FridgeAddRequest;
import com.yoneodoo.api.entity.User;
import com.yoneodoo.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자별 "내 냉장고" 재료 목록을 다루는 서비스입니다.
 * <p>
 * <b>저장 위치</b><br>
 * 별도 테이블이 아니라 {@link User#fridgeIngredients} jsonb 컬럼에 문자열 배열로 저장됩니다.
 * 따라서 모든 작업은 "유저 행을 읽고 → 리스트를 수정하고 → 다시 save" 패턴입니다.
 * <p>
 * <b>트랜잭션</b><br>
 * {@code @Transactional}이 붙은 메서드는 성공 시 커밋, 예외 시 롤백되어 DB 중간 상태가 남지 않게 합니다.
 */
@Service
@RequiredArgsConstructor
public class UserFridgeService {

    /** 유저 행을 읽고 쓰는 유일한 저장소(냉장고도 같은 행 안에 있음). */
    private final UserRepository userRepository;

    /**
     * 요청에 담긴 재료 이름들을 해당 유저의 냉장고 리스트에 추가합니다.
     * <p>
     * 흐름:<br>
     * 1) {@code userId}로 유저 행 조회 — 없으면 예외.<br>
     * 2) {@code fridgeIngredients}가 null이면 빈 리스트로 초기화.<br>
     * 3) 요청의 각 재료 문자열에 대해, 이미 리스트에 없을 때만 add (중복 방지).<br>
     * 4) {@code userRepository.save(user)}로 jsonb 컬럼 변경을 DB에 반영.
     *
     * @param request 유저 ID + 추가할 재료 이름 배열
     */
    @Transactional
    public void addIngredientsToFridge(FridgeAddRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        if (user.getFridgeIngredients() == null) {
            user.setFridgeIngredients(new ArrayList<>());
        }

        for (String newIngredient : request.getIngredients()) {
            if (!user.getFridgeIngredients().contains(newIngredient)) {
                user.getFridgeIngredients().add(newIngredient);
            }
        }

        userRepository.save(user);
        System.out.println("✅ " + request.getUserId() + "번 유저의 냉장고 업데이트 완료!");
    }

    /**
     * 특정 유저의 냉장고 재료 이름 목록을 그대로 반환합니다.
     * <p>
     * null 안전: DB에 빈 값이면 빈 리스트로 돌려 UI가 NPE 없이 동작하게 합니다.
     *
     * @param userId 조회할 회원 PK
     * @return 재료 이름 문자열 리스트(복사본이 아니라 엔티티가 들고 있는 리스트 참조에 가깝지만 읽기 전용 용도로 사용)
     */
    @Transactional(readOnly = true)
    public List<String> getMyFridgeIngredients(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        return user.getFridgeIngredients() != null ? user.getFridgeIngredients() : new ArrayList<>();
    }

    /**
     * 냉장고에서 재료 한 개를 이름 기준으로 제거합니다.
     * <p>
     * URL 등에서 넘어온 {@code ingredientName}과 리스트 원소가 문자열 완전 일치할 때만 remove됩니다.
     * (인코딩·공백 차이에 주의 — 클라이언트와 약속이 필요합니다.)
     *
     * @param userId      회원 PK
     * @param ingredientName 삭제할 재료 텍스트
     */
    @Transactional
    public void removeIngredientFromFridge(Long userId, String ingredientName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        if (user.getFridgeIngredients() != null) {
            user.getFridgeIngredients().remove(ingredientName);
            userRepository.save(user);
            System.out.println("✅ " + userId + "번 유저의 냉장고에서 [" + ingredientName + "] 삭제 완료!");
        }
    }
}
