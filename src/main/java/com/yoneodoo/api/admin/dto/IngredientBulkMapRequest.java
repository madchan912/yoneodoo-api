package com.yoneodoo.api.admin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 승인 모달에서 체크된 항목만 모아 보내는 일괄 매핑 요청입니다.
 */
@Data
public class IngredientBulkMapRequest {

    private List<IngredientBulkMapItem> items = new ArrayList<>();
}
