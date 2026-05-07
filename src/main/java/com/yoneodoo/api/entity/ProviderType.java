package com.yoneodoo.api.entity;

/**
 * {@link User}가 어떤 소셜 계정으로 가입했는지 구분하는 열거형입니다.
 * <p>
 * DB에는 문자열로 저장됩니다({@code @Enumerated(EnumType.STRING)}).
 * 로그인 연동 시 같은 제공자·같은 provider_id인지 조회할 때 사용됩니다.
 */
public enum ProviderType {
    /** 카카오 로그인 */
    KAKAO,
    /** 네이버 로그인 */
    NAVER,
    /** 구글 로그인 */
    GOOGLE
}
