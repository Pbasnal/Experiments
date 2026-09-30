package com.amarkatha.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExperienceSourcePolicyTest {

    private final ExperienceSourcePolicy policy = new ExperienceSourcePolicy();

    @Test
    void usesDemoContentOnlyForAnAdminWhoEnabledIt() {
        assertEquals(ExperienceSource.DEMO, policy.decide(new ExperienceSourceInput(true, true)));
    }

    @Test
    void keepsLiveDataWhenDemoModeIsOff() {
        assertEquals(ExperienceSource.LIVE, policy.decide(new ExperienceSourceInput(true, false)));
    }

    @Test
    void ignoresTheFlagForAnyoneWhoIsNotAnAdmin() {
        assertEquals(ExperienceSource.LIVE, policy.decide(new ExperienceSourceInput(false, true)));
        assertEquals(ExperienceSource.LIVE, policy.decide(new ExperienceSourceInput(false, false)));
    }
}
