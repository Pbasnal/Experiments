package com.amarkatha.identity;

import com.amarkatha.identity.domain.InviteToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {

    Optional<InviteToken> findByToken(String token);

    List<InviteToken> findAllByOrderByCreatedAtDesc();
}
