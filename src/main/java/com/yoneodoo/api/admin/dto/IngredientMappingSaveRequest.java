package com.yoneodoo.api.admin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class IngredientMappingSaveRequest {

    private String masterName;
    private List<String> rawNames = new ArrayList<>();
}
