package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * DB 테이블 {@code users}: 소셜 로그인으로 가입한 회원 한 명을 나타냅니다.
 * <p>
 * <b>냉장고 데이터</b><br>
 * {@code fridge_ingredients} 컬럼은 jsonb 배열로, 사용자가 가진 재료 이름 문자열만 단순히 쌓습니다.
 * 별도의 "냉장고 테이블" 없이 사용자 행 안에 넣는 구조라, 추가/삭제 시 이 엔티티를 읽고 리스트를 수정한 뒤
 * 다시 저장({@code UserRepository.save})하는 흐름입니다.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    /** 회원 고유 ID. 냉장고 API URL 등에서 {@code userId}로 사용됩니다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소셜 로그인 제공자 종류(카카오/네이버/구글 등). {@link ProviderType} 참고. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderType provider;

    /**
     * 해당 제공자 쪽에서 발급한 사용자 식별자(고유값).
     * {@code provider} + {@code provider_id} 조합으로 "이미 가입한 사람인지"를 찾습니다.
     */
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    /** 서비스 내에서 보여 줄 닉네임. */
    @Column(nullable = false, length = 50)
    private String nickname;

    /**
     * 내 냉장고에 들어 있는 재료 이름들의 목록(예: {@code ["계란","고추장"]}).
     * DB에는 jsonb로 저장되며, JPA가 List와 JSON 사이를 변환합니다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fridge_ingredients", columnDefinition = "jsonb")
    private List<String> fridgeIngredients = new ArrayList<>();

    public User(ProviderType provider, String providerId, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
    }
}
