package com.civicconnect.backend.auth;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.civicconnect.backend.exception.NotFoundException;
import com.civicconnect.backend.exception.UnauthorizedException;
import com.civicconnect.backend.exception.ValidationException;

@Service
public class AuthService {

    static final int MIN_PASSWORD_LENGTH = 8;
    static final int MAX_PASSWORD_LENGTH = 72;

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String BAD_CREDENTIALS = "Email or password is incorrect.";

    private final UserDirectory users;
    private final PasswordEncoder encoder;
    private final JwtService tokens;
    private final String decoyHash;

    public AuthService(UserDirectory users, PasswordEncoder encoder, JwtService tokens) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.decoyHash = encoder.encode("decoy-password-for-timing");
    }

    public record Session(String token, UserAccount user) {
    }

    public Session login(String email, String password) {
        String normalized = normalizeEmail(email);
        var account = normalized.isEmpty() ? java.util.Optional.<UserAccount>empty() : users.findAccountByEmail(normalized);
        String candidate = password == null ? "" : password;
        String hash = account.map(UserAccount::passwordHash).orElse(decoyHash);
        boolean matches;
        try {
            matches = encoder.matches(candidate, hash);
        } catch (IllegalArgumentException invalidHash) {
            matches = false;
        }
        if (account.isEmpty() || !matches) {
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        return new Session(tokens.issue(account.get().id()), account.get());
    }

    public Session register(String name, String email, String password) {
        String cleanName = name == null ? "" : name.trim();
        String normalized = normalizeEmail(email);
        if (cleanName.isEmpty()) {
            throw new ValidationException("Enter your name.");
        }
        if (cleanName.length() > 100) {
            throw new ValidationException("Use a name of 100 characters or fewer.");
        }
        if (normalized.length() > 254 || !EMAIL.matcher(normalized).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Use a password with " + MIN_PASSWORD_LENGTH + " to "
                    + MAX_PASSWORD_LENGTH + " characters.");
        }
        UserAccount created = users.createRequester(cleanName, normalized, encoder.encode(password));
        return new Session(tokens.issue(created.id()), created);
    }

    public UserAccount profile(AuthenticatedUser actor) {
        return users.findAccountById(actor.id()).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
