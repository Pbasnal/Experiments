package com.amarkatha.identity;

import com.amarkatha.identity.domain.User;
import com.amarkatha.shared.domain.UserRole;
import java.util.Optional;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserOnboardingService {

    private final UserRepository userRepository;
    private final InviteService inviteService;
    private final FeatureFlagService featureFlagService;
    private final AmarKathaProperties properties;

    public UserOnboardingService(
            UserRepository userRepository,
            InviteService inviteService,
            FeatureFlagService featureFlagService,
            AmarKathaProperties properties
    ) {
        this.userRepository = userRepository;
        this.inviteService = inviteService;
        this.featureFlagService = featureFlagService;
        this.properties = properties;
    }

    @Transactional
    public User completeOAuthLogin(OAuth2User oauth2User, OAuthIntent intent, String pendingInviteToken) {
        String googleSub = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String displayName = oauth2User.getAttribute("name");
        if (googleSub == null || email == null) {
            throw new OAuthOnboardingException(OAuthOnboardingException.Reason.MISSING_PROFILE);
        }

        Optional<User> existing = userRepository.findByGoogleSub(googleSub);
        if (existing.isPresent()) {
            User user = existing.get();
            applyBootstrapAdmin(user, email);
            return userRepository.save(user);
        }

        if (properties.admin().isBootstrapAdmin(email)) {
            return userRepository.save(User.create(googleSub, email, displayName, UserRole.ADMIN));
        }

        return switch (intent) {
            case ADMIN_LOGIN -> throw new OAuthOnboardingException(OAuthOnboardingException.Reason.ADMIN_ACCESS_DENIED);
            case CREATOR_LOGIN -> throw new OAuthOnboardingException(OAuthOnboardingException.Reason.ACCOUNT_NOT_FOUND);
            case GOOGLE_AUTH, CREATOR_SIGNUP -> createCreator(googleSub, email, displayName, pendingInviteToken);
        };
    }

    private User createCreator(String googleSub, String email, String displayName, String pendingInviteToken) {
        if (featureFlagService.isInviteRequired()) {
            if (pendingInviteToken == null || pendingInviteToken.isBlank()) {
                throw new OAuthOnboardingException(OAuthOnboardingException.Reason.INVITE_REQUIRED);
            }
            User user = User.create(googleSub, email, displayName, UserRole.CREATOR);
            user = userRepository.save(user);
            try {
                inviteService.consume(pendingInviteToken, user);
            } catch (InviteInvalidException ex) {
                throw new OAuthOnboardingException(mapInviteReason(ex.getReason()));
            }
            return user;
        }
        return userRepository.save(User.create(googleSub, email, displayName, UserRole.CREATOR));
    }

    private static OAuthOnboardingException.Reason mapInviteReason(InviteInvalidException.InviteInvalidReason reason) {
        return switch (reason) {
            case EXPIRED -> OAuthOnboardingException.Reason.INVITE_EXPIRED;
            case EXHAUSTED, ALREADY_USED -> OAuthOnboardingException.Reason.INVITE_EXHAUSTED;
            case NOT_FOUND, BLANK -> OAuthOnboardingException.Reason.INVITE_INVALID;
        };
    }

    private void applyBootstrapAdmin(User user, String email) {
        if (user.getRole() != UserRole.ADMIN && properties.admin().isBootstrapAdmin(email)) {
            user.setRole(UserRole.ADMIN);
        }
    }
}
