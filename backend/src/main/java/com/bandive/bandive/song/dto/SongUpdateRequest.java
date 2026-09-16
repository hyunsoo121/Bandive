package com.bandive.bandive.song.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.bandive.bandive.song.dto.SongCreateRequest.SessionSlot;

/**
 * 곡 부분 수정 — 제목/아티스트/메모/참고영상/세션 구성. 위시리스트·합주곡 상태 모두 가능. 등록자 본인 또는 관리자만.
 * <p>
 * 제목·아티스트·메모·참고영상: null 필드는 미변경(기존 값 유지). 값이 있으면(빈 문자열 포함) 그대로 반영 —
 * {@code MediaUpdateRequest} 와 동일한 부분수정 컨벤션.
 * <p>
 * {@code sessions}: null 이면 세션 구성 미변경. 값이 있으면 그 목록이 곡의 **전체** 세션 구성이 된다(등록 시와 동일한 의미) —
 * 목록에 없는 악기는 0으로 취급해 줄어든다. 인원이 늘면 빈 슬롯을 추가하고, 줄면 배정 없는 슬롯부터 지운다. 줄이려는 만큼 배정 없는 슬롯이 없으면(이미
 * 멤버/게스트가 배정돼 있으면) 거부한다({@code SESSION_SLOT_ASSIGNED}).
 */
public record SongUpdateRequest(@Size(max = 200, message = "곡 제목은 200자 이내여야 합니다") String title,
		@Size(max = 200) String artist, @Size(max = 2000, message = "메모는 2000자 이내여야 합니다") String memo,
		@Size(max = 500) @Pattern(regexp = "^(https?://.+)?$",
				message = "참고 영상 주소는 http(s):// 로 시작해야 합니다") String referenceVideoUrl,
		@Valid List<SessionSlot> sessions) {
}
