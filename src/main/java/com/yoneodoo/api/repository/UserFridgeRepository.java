package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.UserFridge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFridgeRepository extends JpaRepository<UserFridge, Long> {
}