package com.bandive.bandive.invite;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.common.entity.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "invite_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class InviteCode extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "band_id", nullable = false)
	private Band band;

	/** 초대 링크에 쓰이는 전역 유니크 코드 */
	@Column(nullable = false, unique = true, length = 20)
	private String code;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "max_uses")
	private Integer maxUses;

	@Column(name = "used_count", nullable = false)
	private int usedCount;

	public boolean isExpired() {
		return expiresAt != null && Instant.now().isAfter(expiresAt);
	}

	public boolean isExhausted() {
		return maxUses != null && usedCount >= maxUses;
	}

	public void incrementUsedCount() {
		this.usedCount++;
	}

}
