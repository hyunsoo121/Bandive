package com.bandive.bandive.invite.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.bandive.bandive.invite.InviteCodeRepository;

/**
 * 8자리 초대 코드. 혼동 문자(0/O/1/I)를 뺀 대문자+숫자. 전역 유니크가 깨지면 재시도.
 */
@Component
public class InviteCodeGenerator {

	private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

	private static final int LENGTH = 8;

	private static final int MAX_ATTEMPTS = 10;

	private final SecureRandom random = new SecureRandom();

	private final InviteCodeRepository inviteCodes;

	public InviteCodeGenerator(InviteCodeRepository inviteCodes) {
		this.inviteCodes = inviteCodes;
	}

	public String generateUnique() {
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String candidate = randomCode();
			if (!inviteCodes.existsByCode(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("초대 코드 생성에 반복 실패했습니다.");
	}

	private String randomCode() {
		StringBuilder sb = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
		}
		return sb.toString();
	}

}
