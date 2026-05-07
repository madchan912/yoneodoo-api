package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.ProviderType;
import com.yoneodoo.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * {@link User} 테이블({@code users})에 대한 DB 접근 계층입니다.
 * <p>
 * 소셜 로그인 후 "이미 가입된 사용자인지" 판별할 때,
 * 제공자 종류 + 제공자 측 사용자 ID 조합으로 조회합니다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 동일한 소셜 계정으로 가입한 사용자가 있는지 찾습니다.
     * 있으면 로그인 처리(기존 유저), 없으면 회원가입 플로우로 이어질 수 있습니다.
     *
     * @param provider    카카오/네이버/구글 등
     * @param providerId  해당 플랫폼이 부여한 사용자 식별 문자열
     */
    Optional<User> findByProviderAndProviderId(ProviderType provider, String providerId);
}
