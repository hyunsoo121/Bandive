package com.bandive.bandive.guest.dto;

import com.bandive.bandive.guest.Guest;

public record GuestResponse(Long id, Long bandId, String name) {

	public static GuestResponse from(Guest guest) {
		return new GuestResponse(guest.getId(), guest.getBand().getId(), guest.getName());
	}

}
