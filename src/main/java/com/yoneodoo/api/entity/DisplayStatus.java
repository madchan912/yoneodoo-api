package com.yoneodoo.api.entity;

/**
 * 레시피의 "사용자 노출 여부"를 나타내는 Soft Delete 상태입니다.
 * <p>
 * <b>왜 별도 컬럼인가</b><br>
 * {@link Recipe}의 기존 {@code status} 필드는 크롤링/자막 파이프라인 상태(SUCCESS, NO_SUBTITLES, SKIP …)를 의미하므로,
 * "사용자 화면에 노출할지 말지"를 같은 컬럼으로 다루면 의미가 충돌합니다.
 * 그래서 별도 컬럼 {@code display_status}를 두고, 어드민이 자유롭게 ACTIVE ↔ HIDDEN을 토글할 수 있게 합니다.
 * <p>
 * <b>사용자 화면(GET 등) 정책</b><br>
 * 사용자용 엔드포인트와 재료 검색 캐시는 {@link #ACTIVE}인 레시피만 노출합니다.
 * 어드민 목록·상세는 두 상태 모두 표시합니다.
 */
public enum DisplayStatus {
    /** 일반 사용자에게 노출. 신규 레시피의 기본값. */
    ACTIVE,
    /** Soft Delete. 데이터는 보존하되 사용자 화면에서는 숨김. */
    HIDDEN
}
