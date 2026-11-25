package com.tradex.services;

import com.tradex.exceptions.*;
import com.tradex.models.User;
import com.tradex.repositories.UserRepository;
import com.tradex.utils.PasswordHasher;

import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    static final int MIN_PASSWORD = 8;

    private final UserRepository users;

    public AuthService(UserRepository users) { this.users = users; }

    public User register(String username, String email, String password) throws TradeXException {
        validate(username, email, password);
        String hash = PasswordHasher.hash(password);
        return users.createUserWithWallet(username.trim(), email.trim().toLowerCase(), hash)
                .orElseThrow(() -> new ValidationException("Username or email is already registered"));
    }

    public User login(String usernameOrEmail, String password) throws TradeXException {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.isEmpty()) {
            throw new ValidationException("Enter your username and password");
        }
        String key = usernameOrEmail.trim();
        var creds = users.findCredentials(key.contains("@") ? key.toLowerCase() : key);
        // Same message for unknown user and wrong password to avoid user enumeration.
        if (creds.isEmpty() || !PasswordHasher.verify(password, creds.get().passwordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }
        return creds.get().user();
    }

    static void validate(String username, String email, String password) throws ValidationException {
        if (username == null || !USERNAME.matcher(username.trim()).matches())
            throw new ValidationException("Username must be 3-30 letters, digits or underscores");
        if (email == null || !EMAIL.matcher(email.trim()).matches())
            throw new ValidationException("Enter a valid email address");
        if (password == null || password.length() < MIN_PASSWORD)
            throw new ValidationException("Password must be at least " + MIN_PASSWORD + " characters");
    }
}
