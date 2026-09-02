package com.bandive.bandive.band.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.band.dto.BandCreateRequest;
import com.bandive.bandive.band.dto.BandResponse;
import com.bandive.bandive.band.dto.BandUpdateRequest;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.storage.StorageService;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BandServiceTest extends RepositoryTest {

	@Autowired
	private BandRepository bands;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private UserRepository users;

	@Autowired
	private TestEntityManager em;

	private final RecordingStorage storage = new RecordingStorage();

	private BandService service;

	@BeforeEach
	void setUp() {
		service = new BandService(bands, bandMembers, users, storage);
	}

	@Test
	void 밴드를_만들면_생성자가_OWNER_멤버로_등록된다() {
		Long userId = em.persist(Fixtures.user("owner")).getId();

		BandResponse created = service.create(userId, new BandCreateRequest("자립음악", "인디 밴드"));
		em.flush();
		em.clear();

		assertThat(created.name()).isEqualTo("자립음악");
		assertThat(created.description()).isEqualTo("인디 밴드");
		assertThat(created.memberCount()).isEqualTo(1);
		assertThat(bandMembers.findByBandIdAndUserId(created.id(), userId)).get()
			.extracting(member -> member.getRole())
			.isEqualTo(BandRole.OWNER);
	}

	@Test
	void 없는_유저로_생성하면_404() {
		assertThatThrownBy(() -> service.create(999L, new BandCreateRequest("x", null)))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void 없는_밴드_조회는_404() {
		assertThatThrownBy(() -> service.get(999L)).isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("BAND_NOT_FOUND"));
	}

	@Test
	void 상세_조회는_멤버수를_포함한다() {
		Long ownerId = em.persist(Fixtures.user("o")).getId();
		Long bandId = service.create(ownerId, new BandCreateRequest("팀", null)).id();
		User second = em.persist(Fixtures.user("m"));
		bandMembers.save(Fixtures.member(bands.findById(bandId).orElseThrow(), second, BandRole.MEMBER));
		em.flush();
		em.clear();

		assertThat(service.get(bandId).memberCount()).isEqualTo(2);
	}

	@Test
	void 내_밴드_목록은_내가_속한_것만() {
		Long meId = em.persist(Fixtures.user("me")).getId();
		Long otherId = em.persist(Fixtures.user("other")).getId();
		service.create(meId, new BandCreateRequest("내밴드", null));
		service.create(otherId, new BandCreateRequest("남의밴드", null));
		em.flush();
		em.clear();

		List<BandResponse> mine = service.myBands(meId);
		assertThat(mine).extracting(BandResponse::name).containsExactly("내밴드");
	}

	@Test
	void 정보_수정은_이름과_소개를_바꾼다() {
		Long ownerId = em.persist(Fixtures.user("o")).getId();
		Long bandId = service.create(ownerId, new BandCreateRequest("옛이름", "옛소개")).id();
		em.flush();

		BandResponse updated = service.update(bandId, new BandUpdateRequest("새이름", "새소개"));

		assertThat(updated.name()).isEqualTo("새이름");
		assertThat(updated.description()).isEqualTo("새소개");
	}

	@Test
	void 로고_업로드는_URL_을_저장하고_이전_파일을_지운다() {
		Long ownerId = em.persist(Fixtures.user("o")).getId();
		Long bandId = service.create(ownerId, new BandCreateRequest("팀", null)).id();
		em.flush();

		service.updateLogo(bandId, dummyFile());
		storage.nextUrl = "http://localhost:8081/files/band-logo/second.png";
		BandResponse second = service.updateLogo(bandId, dummyFile());
		em.flush();

		assertThat(second.logoUrl()).isEqualTo("http://localhost:8081/files/band-logo/second.png");
		assertThat(storage.deleted).contains("http://localhost:8081/files/band-logo/first.png");
	}

	private MultipartFile dummyFile() {
		return new org.springframework.mock.web.MockMultipartFile("file", "logo.png", "image/png",
				new byte[] { 1, 2, 3 });
	}

	private static final class RecordingStorage implements StorageService {

		private String nextUrl = "http://localhost:8081/files/band-logo/first.png";

		private final List<String> deleted = new ArrayList<>();

		@Override
		public String store(String directory, MultipartFile file) {
			return nextUrl;
		}

		@Override
		public void delete(String url) {
			if (url != null) {
				deleted.add(url);
			}
		}

	}

}
