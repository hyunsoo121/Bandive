package com.bandive.bandive.member;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.common.entity.BaseTimeEntity;
import com.bandive.bandive.user.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "band_members",
		uniqueConstraints = @UniqueConstraint(name = "uq_band_member", columnNames = { "band_id", "user_id" }))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BandMember extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "band_id", nullable = false)
	private Band band;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BandRole role;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt;

	/** 이 밴드에서 맡은 파트(악기). Instrument 이름이 기본이나 자유 문자열도 허용. */
	@Builder.Default
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "band_member_parts", joinColumns = @JoinColumn(name = "band_member_id"))
	@Column(name = "part", length = 20, nullable = false)
	private Set<String> parts = new LinkedHashSet<>();

	public void changeRole(BandRole role) {
		this.role = role;
	}

	/** 파트 전체 교체 (공백 제거·중복 제거). */
	public void replaceParts(Collection<String> newParts) {
		this.parts.clear();
		if (newParts != null) {
			newParts.stream()
				.filter(part -> part != null)
				.map(String::trim)
				.filter(part -> !part.isEmpty())
				.forEach(this.parts::add);
		}
	}

}
