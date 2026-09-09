package com.bandive.bandive.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.bandive.bandive.common.entity.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 카카오 회원 식별자. LOCAL 가입자는 null. */
	@Column(name = "kakao_id", unique = true, length = 50)
	private String kakaoId;

	@Column(nullable = false, length = 50)
	private String nickname;

	/** LOCAL 가입자의 로그인 아이디. 카카오 가입자는 null (이메일 미수집). */
	@Column(unique = true, length = 255)
	private String email;

	/** BCrypt 해시. LOCAL 가입자만 값이 있다. */
	@Column(name = "password_hash", length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthProvider provider;

	/** 프로필 사진 (자체 스토리지 URL). 미설정이면 null → 프론트는 이니셜 아바타. */
	@Column(name = "avatar_url", length = 500)
	private String avatarUrl;

	public static User ofKakao(String kakaoId, String nickname) {
		return User.builder().kakaoId(kakaoId).nickname(nickname).provider(AuthProvider.KAKAO).build();
	}

	public static User ofLocal(String email, String passwordHash, String nickname) {
		return User.builder()
			.email(email)
			.passwordHash(passwordHash)
			.nickname(nickname)
			.provider(AuthProvider.LOCAL)
			.build();
	}

	/** 닉네임 변경. 호출부에서 trim·검증한 값을 넘긴다. */
	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	/** 비밀번호 해시 교체 (LOCAL 계정만). */
	public void changePassword(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	/** 프로필 사진 URL 설정/해제(null). */
	public void updateAvatar(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public boolean isLocal() {
		return this.provider == AuthProvider.LOCAL && this.passwordHash != null;
	}

}
