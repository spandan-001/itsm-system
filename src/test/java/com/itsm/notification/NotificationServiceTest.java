package com.itsm.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.itsm.enums.NotificationChannel;
import com.itsm.model.Notification;
import com.itsm.repository.NotificationRepository;
import com.itsm.service.notification.EmailNotificationSender;
import com.itsm.service.notification.InAppNotificationSender;
import com.itsm.service.notification.NotificationService;
import com.itsm.service.notification.Recipient;

class NotificationServiceTest {

    @Test
    void sendsThroughEveryRegisteredChannel() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        NotificationService service = new NotificationService(
                List.of(new InAppNotificationSender(repository), new EmailNotificationSender(repository)),
                repository);

        List<Notification> sent = service.notify(
                new Recipient("Asha", "asha@example.test"), "Incident #7 received", "Logged", 7L);

        assertThat(sent).extracting(Notification::getChannel)
                .containsExactly(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        assertThat(sent).allSatisfy(n -> {
            assertThat(n.getRecipientEmail()).isEqualTo("asha@example.test");
            assertThat(n.getIncidentId()).isEqualTo(7L);
            assertThat(n.isRead()).isFalse();
        });
    }
}
