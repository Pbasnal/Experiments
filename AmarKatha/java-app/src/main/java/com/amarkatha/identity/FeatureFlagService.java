package com.amarkatha.identity;

import com.amarkatha.shared.FeatureFlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureFlagService {

    public static final String INVITE_REQUIRED = "invite_required";

    private final FeatureFlagRepository featureFlagRepository;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    @Transactional(readOnly = true)
    public boolean isInviteRequired() {
        return featureFlagRepository.findByName(INVITE_REQUIRED)
                .map(flag -> flag.isEnabled())
                .orElse(true);
    }
}
