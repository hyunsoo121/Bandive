package com.bandive.bandive.band;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "bands")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Band extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(name = "logo_url", length = 500)
	private String logoUrl;

	@Column(name = "banner_url", length = 500)
	private String bannerUrl;

	/** 이름은 비울 수 없고, 소개는 null 로 지울 수 있다. */
	public void updateInfo(String name, String description) {
		if (name != null && !name.isBlank()) {
			this.name = name;
		}
		this.description = description;
	}

	public void changeLogo(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public void changeBanner(String bannerUrl) {
		this.bannerUrl = bannerUrl;
	}

}
