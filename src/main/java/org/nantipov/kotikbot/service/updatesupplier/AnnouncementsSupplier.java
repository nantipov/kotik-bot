package org.nantipov.kotikbot.service.updatesupplier;

import org.nantipov.kotikbot.domain.CollectedMessage;
import org.nantipov.kotikbot.domain.CollectedMessages;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.domain.entity.Announcement;
import org.nantipov.kotikbot.respository.AnnouncementsRepository;
import org.nantipov.kotikbot.service.UpdatesDistributionService;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AnnouncementsSupplier implements UpdateSupplier {

    private static final String SUPPLIER_ID = "AnnouncementsSupplier";

    private final UpdatesDistributionService updatesService;
    private final AnnouncementsRepository announcementsRepository;

    private int lastId = -1;

    public AnnouncementsSupplier(UpdatesDistributionService updatesService,
                                 AnnouncementsRepository announcementsRepository) {
        this.updatesService = updatesService;
        this.announcementsRepository = announcementsRepository;
    }

    @Override
    public void deliver() {
        var announcements = announcementsRepository.findByIdGreaterThan(lastId);
        announcements.stream()
                     .collect(Collectors.groupingBy(Announcement::getGroupId))
                     .forEach(this::deliverAnnouncements);

        lastId = announcements.stream()
                              .mapToInt(Announcement::getId)
                              .max()
                              .orElse(-1);
    }

    private void deliverAnnouncements(String groupId, List<Announcement> announcementsByGroups) {
        if (announcementsByGroups.isEmpty() || updatesService.isAlreadyDelivered(SUPPLIER_ID, groupId)) {
            return;
        }

        updatesService.storeUpdate(
                SUPPLIER_ID, groupId, OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(1),
                new CollectedMessages(
                        announcementsByGroups.stream()
                                             .map(this::toMessage)
                                             .collect(Collectors.toList())
                )
        );
    }

    private CollectedMessage toMessage(Announcement announcement) {
        var message = new SupplierMessage();
        message.setMarkdownText(announcement.getAnnouncementMarkdown());
        return new CollectedMessage(announcement.getLanguage(), true, message);
    }
}
