package org.nantipov.kotikbot.service.updatesupplier;

import lombok.extern.slf4j.Slf4j;
import org.nantipov.kotikbot.domain.CollectedMessage;
import org.nantipov.kotikbot.domain.CollectedMessages;
import org.nantipov.kotikbot.domain.RoomLanguage;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.service.FeedService;
import org.nantipov.kotikbot.service.UpdatesDistributionService;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.nantipov.kotikbot.service.Utils.escapeReservedCharacters;

@Slf4j
@Component
public class OfficeHolidaysSupplier implements UpdateSupplier {

    private static final String SUPPLIER_ID = "OfficeHolidaysSupplier";
    private static final String FEED_URL = "https://www.officeholidays.com/rss/all_holidays";

    private final UpdatesDistributionService updatesService;
    private final FeedService feedService;

    public OfficeHolidaysSupplier(UpdatesDistributionService updatesService, FeedService feedService) {
        this.updatesService = updatesService;
        this.feedService = feedService;
    }

    @Override
    public void deliver() {
        deliverPublicHolidayToday();
    }

    private void deliverPublicHolidayToday() {
        var today = OffsetDateTime.now(ZoneId.of(ZoneOffset.UTC.getId())).truncatedTo(ChronoUnit.DAYS);
        String updateKey = "PHT" + DateTimeFormatter.BASIC_ISO_DATE.format(today);
        if (updatesService.isAlreadyDelivered(SUPPLIER_ID, updateKey)) {
            return;
        }
        holiday(today)
                .ifPresent(message ->
                                   updatesService.storeUpdate(
                                           SUPPLIER_ID, updateKey, today.plusHours(23).plusMinutes(59),
                                           new CollectedMessages(
                                                   List.of(new CollectedMessage(RoomLanguage.EN, message))
                                           )
                                   )
                );
    }

    private Optional<SupplierMessage> holiday(OffsetDateTime day) {
        return feedService.feed(FEED_URL, FeedService.TODAY_ENTRIES, entry -> {
            var supplierMessage = new SupplierMessage();
            supplierMessage.setMarkdownText(
                    "*Public holiday today :: _" +
                    escapeReservedCharacters(DateTimeFormatter.ISO_LOCAL_DATE.format(day)) +
                    "_*\n\n" +
                    escapeReservedCharacters(entry.getTitle())
            );
            return supplierMessage;
        }).findAny();
    }
}
