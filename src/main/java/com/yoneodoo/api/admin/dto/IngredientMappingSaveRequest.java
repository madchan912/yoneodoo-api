package com.yoneodoo.api.admin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 어드민이 "여러 원본 재료를 한 마스터 이름으로 묶어" 저장할 때 보내는 요청 본문입니다.
 * <p>
 * {@code masterName}: 대표로 삼을 표준 재료명(서버에서 정규화됨).<br>
 * {@code rawNames}: 묶을 원본 키들의 목록(각각 정규화되어 매핑 행으로 저장/갱신).
 */
@Data
public class IngredientMappingSaveRequest {

    /** 마스터(표준) 재료명. */
    private String masterName;
    /** 함께 묶일 원본 재료명들. */
    private List<String> rawNames = new ArrayList<>();
}
