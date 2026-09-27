package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.NotificationPreference;
import com.amarkatha.engagement.dto.NotificationCapabilitiesDto;
import com.amarkatha.engagement.dto.NotificationPreferenceDto;
import com.amarkatha.engagement.dto.NotificationPreferenceUpdateRequest;
import com.amarkatha.shared.ReaderFeatureGate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationMailSender mailSender;
    private final ReaderFeatureGate readerFeatureGate;

    public NotificationPreferenceService(
            NotificationPreferenceRepository preferenceRepository,
            NotificationMailSender mailSender,
            ReaderFeatureGate readerFeatureGate
    ) {
        this.preferenceRepository = preferenceRepository;
        this.mailSender = mailSender;
        this.readerFeatureGate = readerFeatureGate;
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceDto getPreferences(UUID userId) {
        return toDto(resolve(userId));
    }

    @Transactional
    public NotificationPreferenceDto updatePreferences(UUID userId, NotificationPreferenceUpdateRequest request) {
        Boolean emailNewChapter = request == null ? null : request.emailNewChapter();
        Boolean inAppNewChapter = request == null ? null : request.inAppNewChapter();
        Boolean emailProductUpdates = request == null ? null : request.emailProductUpdates();

        boolean emailCapable = readerFeatureGate.emailNotificationsEnabled(mailSender.isEnabled());
        if (!emailCapable
                && (Boolean.TRUE.equals(emailNewChapter) || Boolean.TRUE.equals(emailProductUpdates))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email notifications are not available"
            );
        }
        if (!readerFeatureGate.inAppNotificationsEnabled() && Boolean.TRUE.equals(inAppNewChapter)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "In-app notifications are not available"
            );
        }

        NotificationPreference preference = preferenceRepository.findById(userId)
                .orElseGet(() -> NotificationPreference.defaults(userId));
        preference.update(emailNewChapter, inAppNewChapter, emailProductUpdates);
        return toDto(preferenceRepository.save(preference));
    }

    @Transactional(readOnly = true)
    public NotificationPreference resolve(UUID userId) {
        return preferenceRepository.findById(userId)
                .orElseGet(() -> NotificationPreference.defaults(userId));
    }

    public NotificationCapabilitiesDto capabilities() {
        return new NotificationCapabilitiesDto(
                readerFeatureGate.inAppNotificationsEnabled(),
                readerFeatureGate.emailNotificationsEnabled(mailSender.isEnabled()),
                false
        );
    }

    private static NotificationPreferenceDto toDto(NotificationPreference preference) {
        return new NotificationPreferenceDto(
                preference.isEmailNewChapter(),
                preference.isInAppNewChapter(),
                preference.isEmailProductUpdates(),
                preference.getUpdatedAt()
        );
    }
}
