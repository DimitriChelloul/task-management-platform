package org.dimitri.user.application;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email) {
        super("Email already used: " + email);
    }
}
