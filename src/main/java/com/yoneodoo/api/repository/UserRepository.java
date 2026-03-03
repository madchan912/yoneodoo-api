package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.ProviderType;
import com.yoneodoo.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 소셜 로그인 시 가입된 유저인지 확인하는 마법의 주문!
    Optional<User> findByProviderAndProviderId(ProviderType provider, String providerId);
}