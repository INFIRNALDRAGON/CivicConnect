package com.civicconnect.backend.auth;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.civicconnect.backend.exception.ConflictException;

@Component
class JpaUserDirectory implements UserDirectory {

    private final UserRepository users;

    JpaUserDirectory(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> findById(UUID id) {
        return users.findById(id)
                .map(u -> new AuthenticatedUser(u.id, Role.valueOf(u.role), u.categoryIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findAccountById(UUID id) {
        return users.findById(id).map(JpaUserDirectory::toAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findAccountByEmail(String email) {
        return users.findByEmailIgnoreCase(email).map(JpaUserDirectory::toAccount);
    }

    @Override
    @Transactional
    public UserAccount createRequester(String name, String email, String passwordHash) {
        UserEntity entity = new UserEntity();
        entity.name = name;
        entity.email = email;
        entity.passwordHash = passwordHash;
        entity.role = Role.REQUESTER.name();
        try {
            return toAccount(users.saveAndFlush(entity));
        } catch (DataIntegrityViolationException duplicate) {
            throw new ConflictException("An account with this email already exists.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> namesOf(Collection<UUID> ids) {
        Map<UUID, String> names = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return names;
        }
        users.findByIdIn(ids).forEach(u -> names.put(u.id, u.name));
        return names;
    }

    private static UserAccount toAccount(UserEntity u) {
        return new UserAccount(u.id, u.name, u.email, u.passwordHash, Role.valueOf(u.role));
    }
}
