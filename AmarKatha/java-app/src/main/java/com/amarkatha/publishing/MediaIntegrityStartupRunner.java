package com.amarkatha.publishing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On boot: warn (and optionally reconcile) when listed chapters reference missing media.
 */
@Component
public class MediaIntegrityStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MediaIntegrityStartupRunner.class);

    private final MediaReconcileService mediaReconcileService;
    private final boolean reconcileOnStartup;

    public MediaIntegrityStartupRunner(
            MediaReconcileService mediaReconcileService,
            @Value("${amarkatha.media.reconcile-on-startup:true}") boolean reconcileOnStartup
    ) {
        this.mediaReconcileService = mediaReconcileService;
        this.reconcileOnStartup = reconcileOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        int broken = mediaReconcileService.countBrokenListedChapters();
        if (broken == 0) {
            log.info("Media integrity check: all listed chapters have media files");
            return;
        }
        log.warn("Media integrity check: {} listed chapter(s) have missing media files", broken);
        if (reconcileOnStartup) {
            MediaReconcileService.ReconcileReport report = mediaReconcileService.reconcile();
            log.warn(
                    "Startup media reconcile delisted={} relisted={}",
                    report.delisted().size(),
                    report.relisted().size()
            );
        } else {
            log.warn(
                    "Set amarkatha.media.reconcile-on-startup=true or run Admin → Media to delist broken chapters"
            );
        }
    }
}
