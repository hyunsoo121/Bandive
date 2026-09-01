package com.bandive.bandive.support;

import java.time.Instant;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.schedule.Schedule;
import com.bandive.bandive.schedule.ScheduleType;
import com.bandive.bandive.song.Song;
import com.bandive.bandive.song.SongPart;
import com.bandive.bandive.song.SongSourceType;
import com.bandive.bandive.song.SongStatus;
import com.bandive.bandive.song.Instrument;
import com.bandive.bandive.user.User;

/**
 * 테스트용 엔티티 빌더 모음. 필수 필드만 채운 상태로 반환하며, 저장은 호출부에서 한다.
 */
public final class Fixtures {

	private Fixtures() {
	}

	public static User user(String kakaoId) {
		return User.builder().kakaoId(kakaoId).nickname("nick-" + kakaoId).email(kakaoId + "@example.com").build();
	}

	public static Band band(String name) {
		return Band.builder().name(name).build();
	}

	public static BandMember member(Band band, User user, BandRole role) {
		return BandMember.builder().band(band).user(user).role(role).joinedAt(Instant.now()).build();
	}

	public static Song song(Band band, User addedBy, SongStatus status) {
		return Song.builder()
			.band(band)
			.addedBy(addedBy)
			.title("song-title")
			.artist("artist")
			.status(status)
			.sourceType(SongSourceType.MANUAL)
			.build();
	}

	public static SongPart part(Instrument instrument, int partIndex) {
		return SongPart.builder().instrument(instrument).partIndex(partIndex).build();
	}

	public static Schedule schedule(Band band, User createdBy) {
		return Schedule.builder()
			.band(band)
			.createdBy(createdBy)
			.type(ScheduleType.REHEARSAL)
			.dateTime(Instant.now())
			.location("연습실 A")
			.build();
	}

}
