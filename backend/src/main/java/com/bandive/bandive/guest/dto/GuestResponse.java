package com.bandive.bandive.guest.dto;

import com.bandive.bandive.guest.Guest;

public record GuestResponse(Long id, Long bandId, String name, String session) {

	public static GuestResponse from(Guest guest) {
		return new GuestResponse(guest.getId(), guest.getBand().getId(), guest.getName(), guest.getSession());
	}

}
