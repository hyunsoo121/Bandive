package com.bandive.bandive.invite.service;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.dto.BandResponse;
import com.bandive.bandive.band.service.BandService;
import com.bandive.bandive.band.dto.BandCreateRequest;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.invite.InviteCodeRepository;
import com.bandive.bandive.invite.dto.InviteCodeResponse;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.support.IntegrationTest;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InviteServiceTest extends IntegrationTest {

	@Autowired
	private InviteService inviteService;

	@Autowired
	private BandService bandService;

	@Autowired
	private InviteCodeRepository inviteCodes;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private UserRepository users;

	@Autowired
	private StringRedisTemplate redis;

	private Long ownerId;

	private Long bandId;

	@BeforeEach
	void setUp() {
		ownerId = users.save(User.builder().kakaoId("owner-" + UUID.randomUUID()).nickname("장").build()).getId();
		bandId = bandService.create(ownerId, new BandCreateRequest("밴드-" + UUID.randomUUID(), null)).id();
	}

	private Long newUser() {
		return users.save(User.builder().kakaoId("u-" + UUID.randomUUID()).nickname("멤버").build()).getId();
	}

	@Test
	void 발급하면_코드와_링크를_주고_Redis_에_저장된다() {
		InviteCodeResponse response = inviteService.issue(bandId);

		assertThat(response.code()).isNotBlank().hasSize(8);
		assertThat(response.inviteUrl()).isEqualTo("http://localhost:5173/invite/" + response.code());
		assertThat(response.usedCount()).isZero();
		assertThat(redis.opsForValue().get("invite:" + response.code())).isEqualTo(String.valueOf(bandId));
	}

	@Test
	void 재발급하면_이전_코드는_DB_와_Redis_에서_모두_사라진다() {
		String first = inviteService.issue(bandId).code();
		String second = inviteService.issue(bandId).code();

		assertThat(second).isNotEqualTo(first);
		assertThat(inviteCodes.findByCode(first)).isEmpty();
		assertThat(redis.opsForValue().get("invite:" + first)).isNull();
		assertThat(inviteCodes.findByBandId(bandId)).get().extracting(code -> code.getCode()).isEqualTo(second);
		assertThat(redis.opsForValue().get("invite:" + second)).isEqualTo(String.valueOf(bandId));
	}

	@Test
	void 코드로_가입하면_MEMBER_로_등록되고_used_count_가_증가한다() {
		String code = inviteService.issue(bandId).code();
		Long joinerId = newUser();

		BandResponse joined = inviteService.join(joinerId, code);

		assertThat(joined.id()).isEqualTo(bandId);
		assertThat(joined.memberCount()).isEqualTo(2);
		assertThat(bandMembers.findByBandIdAndUserId(bandId, joinerId)).get()
			.extracting(member -> member.getRole())
			.isEqualTo(BandRole.MEMBER);
		assertThat(inviteCodes.findByCode(code).orElseThrow().getUsedCount()).isEqualTo(1);
	}

	@Test
	void 없는_코드로_가입하면_404() {
		assertThatThrownBy(() -> inviteService.join(newUser(), "ZZZZ9999")).isInstanceOf(NotFoundException.class);
	}

	@Test
	void 이미_멤버가_가입하면_409() {
		String code = inviteService.issue(bandId).code();

		assertThatThrownBy(() -> inviteService.join(ownerId, code)).isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("ALREADY_MEMBER"));
	}

	@Test
	void Redis_가_비어있어도_DB_에서_복구해_가입된다() {
		String code = inviteService.issue(bandId).code();
		redis.delete("invite:" + code);

		BandResponse joined = inviteService.join(newUser(), code);

		assertThat(joined.id()).isEqualTo(bandId);
		assertThat(redis.opsForValue().get("invite:" + code)).isEqualTo(String.valueOf(bandId));
	}

}
