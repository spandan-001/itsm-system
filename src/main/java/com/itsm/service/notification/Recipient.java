package com.itsm.service.notification;

import com.itsm.model.User;

/** Who a notification goes to. Works for any User subclass. */
public record Recipient(String name, String email) {

    public static Recipient of(User user) {
        return new Recipient(user.getName(), user.getEmail());
    }
}
