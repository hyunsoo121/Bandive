package com.bandive.bandive.notification;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/** 최근 알림 목록 — band/actor fetch join (닉네임·밴드명 표시용). */
	@Query("""
			select n from Notification n
			  join fetch n.band
			  left join fetch n.actor
			where n.recipient.id = :recipientId
			order by n.createdAt desc
			""")
	List<Notification> findRecentByRecipient(Long recipientId, Limit limit);

	long countByRecipientIdAndReadAtIsNull(Long recipientId);

	List<Notification> findAllByRecipientIdAndReadAtIsNull(Long recipientId);

}
