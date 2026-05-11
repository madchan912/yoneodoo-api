package com.yoneodoo.api.admin.dto;

import lombok.Data;

/**
 * 일괄 매핑 한 줄: 원본 재료 키 → 마스터 재료명.
 */
@Data
public class IngredientBulkMapItem {

    private String rawName;
    private String masterName;
}
