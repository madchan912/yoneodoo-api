package com.yoneodoo.api.admin.dto;

import lombok.Data;

/**
 * {@code POST /api/v1/admin/youtubers} — 유튜버 등록 요청 본문입니다.
 * 채널 URL과 어드민용 표시명만 받습니다.
 */
@Data
public class WatchedYoutuberRequest {

    /** 유튜브 채널 URL. 예: {@code https://www.youtube.com/@유지만} */
    private String channelUrl;

    /** 어드민 화면에서 쓸 표시명. 레시피 youtuber_name 과 맞추면 통계 연동이 됩니다. */
    private String youtuberName;
}
