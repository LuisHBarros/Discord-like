package com.luishbarros.discord_like.modules.identity.domain.model;

import com.luishbarros.discord_like.modules.identity.domain.model.error.InvalidUserError;
import com.luishbarros.discord_like.modules.identity.domain.model.value_object.Email;
import com.luishbarros.discord_like.modules.identity.domain.model.value_object.PasswordHash;
import com.luishbarros.discord_like.modules.identity.domain.model.value_object.Username;
import com.luishbarros.discord_like.shared.domain.model.BaseEntity;
import java.time.Instant;

public class User extends BaseEntity {

    private Username username;
    private Email email;
    private PasswordHash passwordHash;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    protected User() {}

    public User(Username username, Email email, PasswordHash passwordHash, Instant createdAt) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = true;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static User reconstitute(Long id, Username username, Email email, PasswordHash passwordHash, boolean active, Instant createdAt, Instant updatedAt) {
        User user = new User();
        user.id = id;
        user.username = username;
        user.email = email;
        user.passwordHash = passwordHash;
        user.active = active;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }

    public void changePassword(PasswordHash newPasswordHash, Instant updatedAt) {
        if (!active) {
            throw new InvalidUserError("Cannot change password of inactive user");
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = updatedAt;
    }

    public void deactivate(Instant updatedAt) {
        if (!active) {
            throw new InvalidUserError("User is already inactive");
        }
        this.active = false;
        this.updatedAt = updatedAt;
    }

    public void activate(Instant updatedAt) {
        if (active) {
            throw new InvalidUserError("User is already active");
        }
        this.active = true;
        this.updatedAt = updatedAt;
    }

    public Username getUsername()     { return username; }
    public Email getEmail()            { return email; }
    public PasswordHash getPasswordHash() { return passwordHash; }
    public boolean isActive()         { return active; }
    public Instant getCreatedAt()     { return createdAt; }
    public Instant getUpdatedAt()     { return updatedAt; }
}
