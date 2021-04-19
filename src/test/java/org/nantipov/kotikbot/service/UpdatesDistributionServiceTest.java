package org.nantipov.kotikbot.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nantipov.kotikbot.domain.CollectedMessage;
import org.nantipov.kotikbot.domain.CollectedMessages;
import org.nantipov.kotikbot.domain.RoomLanguage;
import org.nantipov.kotikbot.domain.RoomSettings;
import org.nantipov.kotikbot.domain.SupplierMessage;

import java.time.ZoneId;
import java.util.List;

public class UpdatesDistributionServiceTest {

    final UpdatesDistributionService service = new UpdatesDistributionService(null, null, null, null, null, null, 0);

    @Test
    void filtersByLanguages() {
        var messageEn = new SupplierMessage();
        messageEn.setMarkdownText("EN");

        var messageRu = new SupplierMessage();
        messageEn.setMarkdownText("RU");

        var collectedMessages = new CollectedMessages(
                List.of(
                        new CollectedMessage(RoomLanguage.RU, false, messageRu),
                        new CollectedMessage(RoomLanguage.EN, false, messageEn)
                )
        );

        var settings = RoomSettings.builder()
                                   .languages(List.of(RoomLanguage.EN, RoomLanguage.RU))
                                   .timeZoneId(ZoneId.systemDefault())
                                   .acceptBetaFeatures(false)
                                   .build();
        var filteredMessages = service.getMessagesByRoomSettings(collectedMessages, settings);

        Assertions.assertEquals(filteredMessages.size(), 1);
        Assertions.assertEquals(filteredMessages.get(0), messageEn);
    }

    @Test
    void filtersByLanguagesAndBetaFlag() {
        var messageEn = new SupplierMessage();
        messageEn.setMarkdownText("EN");

        var messageRu = new SupplierMessage();
        messageEn.setMarkdownText("RU");

        var collectedMessages = new CollectedMessages(
                List.of(
                        new CollectedMessage(RoomLanguage.RU, false, messageRu),
                        new CollectedMessage(RoomLanguage.EN, true, messageEn)
                )
        );

        var settings = RoomSettings.builder()
                                   .languages(List.of(RoomLanguage.EN, RoomLanguage.RU))
                                   .timeZoneId(ZoneId.systemDefault())
                                   .acceptBetaFeatures(false)
                                   .build();
        var filteredMessages = service.getMessagesByRoomSettings(collectedMessages, settings);

        Assertions.assertTrue(filteredMessages.isEmpty());
    }

    @Test
    void acceptsBetaMessages() {
        var messageEn = new SupplierMessage();
        messageEn.setMarkdownText("EN");

        var messageRu = new SupplierMessage();
        messageEn.setMarkdownText("RU");

        var collectedMessages = new CollectedMessages(
                List.of(
                        new CollectedMessage(RoomLanguage.RU, true, messageRu),
                        new CollectedMessage(RoomLanguage.EN, true, messageEn)
                )
        );

        var settings = RoomSettings.builder()
                                   .languages(List.of(RoomLanguage.EN, RoomLanguage.RU))
                                   .timeZoneId(ZoneId.systemDefault())
                                   .acceptBetaFeatures(true)
                                   .build();
        var filteredMessages = service.getMessagesByRoomSettings(collectedMessages, settings);

        Assertions.assertEquals(filteredMessages.size(), 1);
        Assertions.assertEquals(filteredMessages.get(0), messageEn);
    }

    @Test
    void skipsBetaMessages() {
        var messageEn = new SupplierMessage();
        messageEn.setMarkdownText("EN");

        var messageRu = new SupplierMessage();
        messageEn.setMarkdownText("RU");

        var collectedMessages = new CollectedMessages(
                List.of(
                        new CollectedMessage(RoomLanguage.RU, true, messageRu),
                        new CollectedMessage(RoomLanguage.EN, true, messageEn)
                )
        );

        var settings = RoomSettings.builder()
                                   .languages(List.of(RoomLanguage.EN, RoomLanguage.RU))
                                   .timeZoneId(ZoneId.systemDefault())
                                   .acceptBetaFeatures(false)
                                   .build();
        var filteredMessages = service.getMessagesByRoomSettings(collectedMessages, settings);

        Assertions.assertTrue(filteredMessages.isEmpty());
    }
}
