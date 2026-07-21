package org.dimitri.user.application;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier);
    }
}
