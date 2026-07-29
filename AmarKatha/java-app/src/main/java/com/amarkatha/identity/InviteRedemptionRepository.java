package com.amarkatha.identity;

import com.amarkatha.identity.domain.InviteRedemption;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InviteRedemptionRepository extends JpaRepository<InviteRedemption, UUID> {

    @Query("""
            select r from InviteRedemption r
            join fetch r.user
            join fetch r.inviteToken
            where r.inviteToken.id in :inviteIds
            order by r.redeemedAt asc
            """)
    List<InviteRedemption> findByInviteTokenIdInWithUser(@Param("inviteIds") Collection<UUID> inviteIds);
}
