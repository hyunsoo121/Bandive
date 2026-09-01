package com.bandive.bandive.media;

/**
 * 공개 범위. 비회원 GET 시 MEMBERS_ONLY 영상은 제외된다.
 */
public enum MediaVisibility {

	/** 밴드 멤버만 */
	MEMBERS_ONLY,
	/** 링크 소지자 누구나 */
	LINK_PUBLIC

}
