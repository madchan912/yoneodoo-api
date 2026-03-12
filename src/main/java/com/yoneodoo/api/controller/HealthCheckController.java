package com.yoneodoo.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    // 브라우저나 로드밸런서(AWS ALB), 쿠버네티스가 질러볼 주소!
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
