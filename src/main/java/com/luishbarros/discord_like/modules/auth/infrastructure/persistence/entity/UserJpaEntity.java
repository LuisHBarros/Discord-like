// auth/infrastructure/persistence/entity/UserJpaEntity.java
package com.luishbarros.discord_like.modules.auth.infrastructure.persistence.entity;

import com.luishbarros.discord_like.modules.auth.domain.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {}

    public UserJpaEntity(String username, String email, String passwordHash, Instant createdAt) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = true;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public User toDomain() {
        return User.reconstitute(id, username, email, passwordHash, active, createdAt, updatedAt);
    }

    public static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity(
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getCreatedAt()
        );
        entity.id = user.getId();
        entity.active = user.isActive();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }
}