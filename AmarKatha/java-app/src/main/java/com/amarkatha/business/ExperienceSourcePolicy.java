package com.amarkatha.business;

/**
 * Demo content is an admin preview, not a separate account role.
 * A stored flag on any other role stays on live data.
 */
public final class ExperienceSourcePolicy {

    public ExperienceSource decide(ExperienceSourceInput input) {
        return input.admin() && input.demoModeRequested()
                ? ExperienceSource.DEMO
                : ExperienceSource.LIVE;
    }

    public boolean demo(boolean admin, boolean demoModeRequested) {
        return decide(new ExperienceSourceInput(admin, demoModeRequested)) == ExperienceSource.DEMO;
    }
}
