package com.amarkatha.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.engagement.domain.NotificationPreference;
import com.amarkatha.engagement.dto.NotificationPreferenceDto;
import com.amarkatha.engagement.dto.NotificationPreferenceUpdateRequest;
import com.amarkatha.shared.ReaderFeatureGate;
import com.amarkatha.shared.ReaderFeatureProperties;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    @Mock
    private NotificationMailSender mailSender;

    private NotificationPreferenceService service;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceService(
                preferenceRepository,
                mailSender,
                new ReaderFeatureGate(new ReaderFeatureProperties(true, true, true, true, true))
        );
    }

    @Test
    void defaultsWhenNoRow() {
        UUID userId = UUID.randomUUID();
        when(preferenceRepository.findById(userId)).thenReturn(Optional.empty());

        NotificationPreferenceDto dto = service.getPreferences(userId);

        assertFalse(dto.emailNewChapter());
        assertTrue(dto.inAppNewChapter());
        assertFalse(dto.emailProductUpdates());
    }

    @Test
    void updatePersistsOptInWhenMailEnabled() {
        UUID userId = UUID.randomUUID();
        when(mailSender.isEnabled()).thenReturn(true);
        when(preferenceRepository.findById(userId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NotificationPreferenceDto dto = service.updatePreferences(
                userId,
                new NotificationPreferenceUpdateRequest(true, true, false)
        );

        assertTrue(dto.emailNewChapter());
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void updateRejectsEmailEnableWhenMailDisabled() {
        UUID userId = UUID.randomUUID();
        when(mailSender.isEnabled()).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.updatePreferences(
                        userId,
                        new NotificationPreferenceUpdateRequest(true, true, false)
                )
        );

        assertEquals(400, ex.getStatusCode().value());
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void updateRejectsProductEmailEnableWhenMailDisabled() {
        UUID userId = UUID.randomUUID();
        when(mailSender.isEnabled()).thenReturn(false);

        assertThrows(
                ResponseStatusException.class,
                () -> service.updatePreferences(
                        userId,
                        new NotificationPreferenceUpdateRequest(false, true, true)
                )
        );
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void updateAllowsInAppOnlyWhenMailDisabled() {
        UUID userId = UUID.randomUUID();
        when(mailSender.isEnabled()).thenReturn(false);
        when(preferenceRepository.findById(userId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NotificationPreferenceDto dto = service.updatePreferences(
                userId,
                new NotificationPreferenceUpdateRequest(false, false, false)
        );

        assertFalse(dto.emailNewChapter());
        assertFalse(dto.inAppNewChapter());
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void capabilitiesReflectMailFlagAndFeatureGate() {
        when(mailSender.isEnabled()).thenReturn(false);
        assertFalse(service.capabilities().emailAvailable());
        when(mailSender.isEnabled()).thenReturn(true);
        assertTrue(service.capabilities().emailAvailable());
        assertEquals(true, service.capabilities().inAppAvailable());
        assertEquals(false, service.capabilities().pushAvailable());
    }

    @Test
    void capabilitiesHideEmailWhenFeatureDisabled() {
        service = new NotificationPreferenceService(
                preferenceRepository,
                mailSender,
                new ReaderFeatureGate(new ReaderFeatureProperties(true, true, true, true, false))
        );
        when(mailSender.isEnabled()).thenReturn(true);
        assertFalse(service.capabilities().emailAvailable());
    }
}
