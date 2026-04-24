package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.AdminDashboardStatsResponse;
import com.yoneodoo.api.admin.dto.AdminRecipeRowResponse;
import com.yoneodoo.api.admin.dto.IngredientMappingSaveRequest;
import com.yoneodoo.api.admin.dto.UnclassifiedIngredientRowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "https://yoneodoo.vercel.app"})
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard/stats")
    public AdminDashboardStatsResponse dashboardStats() {
        return adminService.dashboardStats();
    }

    @GetMapping("/recipes")
    public List<AdminRecipeRowResponse> listRecipes(
            @RequestParam(name = "filter", defaultValue = "all") String filter
    ) {
        return adminService.listRecipesForAdmin(filter);
    }

    @GetMapping("/ingredients/unclassified")
    public List<UnclassifiedIngredientRowResponse> unclassifiedIngredients() {
        return adminService.listUnclassifiedIngredients();
    }

    @PostMapping("/ingredients/mapping")
    public Map<String, Object> saveIngredientMapping(@RequestBody IngredientMappingSaveRequest body) {
        try {
            int updated = adminService.saveIngredientMappings(body);
            return Map.of("updated", updated);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
