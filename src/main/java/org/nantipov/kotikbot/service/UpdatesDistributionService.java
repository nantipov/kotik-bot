package org.nantipov.kotikbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.nantipov.kotikbot.domain.CollectedMessage;
import org.nantipov.kotikbot.domain.CollectedMessages;
import org.nantipov.kotikbot.domain.RoomLanguage;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.domain.entity.CollectedUpdate;
import org.nantipov.kotikbot.domain.entity.DistributedUpdate;
import org.nantipov.kotikbot.domain.entity.Room;
import org.nantipov.kotikbot.respository.CollectedUpdateRepository;
import org.nantipov.kotikbot.respository.DistributedUpdateRepository;
import org.nantipov.kotikbot.respository.RoomRepository;
import org.nantipov.kotikbot.service.telegram.TelegramBot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UpdatesDistributionService {

    private final CollectedUpdateRepository collectedUpdateRepository;
    private final RoomRepository roomRepository;
    private final DistributedUpdateRepository distributedUpdateRepository;
    private final ObjectMapper objectMapper;
    private final RoomSettingsService roomSettingsService;
    private final TelegramBot telegramBot;
    private final int intervalPerMessagePerChatMinutes;

    public UpdatesDistributionService(CollectedUpdateRepository collectedUpdateRepository,
                                      RoomRepository roomRepository,
                                      DistributedUpdateRepository distributedUpdateRepository,
                                      @Qualifier("kotik") ObjectMapper objectMapper,
                                      RoomSettingsService roomSettingsService,
                                      TelegramBot telegramBot,
                                      @Value("${kotik.interval-per-message-per-chat-minutes}")
                                              int intervalPerMessagePerChatMinutes) {
        this.collectedUpdateRepository = collectedUpdateRepository;
        this.roomRepository = roomRepository;
        this.distributedUpdateRepository = distributedUpdateRepository;
        this.objectMapper = objectMapper;
        this.roomSettingsService = roomSettingsService;
        this.telegramBot = telegramBot;
        this.intervalPerMessagePerChatMinutes = intervalPerMessagePerChatMinutes;
    }

    @Cacheable(value = "already-delivered", unless = "!#result")
    public boolean isAlreadyDelivered(String supplierId, String updateKey) {
        return collectedUpdateRepository.existsBySupplierAndUpdateKey(supplierId, updateKey);
    }

    public void storeUpdate(String supplierId, String updateKey, OffsetDateTime actualTill,
                            CollectedMessages collectedMessages) {
        try {
            log.info("Storing update {} / {}", supplierId, updateKey);
            var update = new CollectedUpdate();
            update.setSupplier(supplierId);
            update.setUpdateKey(updateKey);
            update.setActualTill(actualTill);
            update.setMessageJson(objectMapper.writeValueAsString(new SupplierMessage()));//TODO backward compatibility
            update.setMessagesJson(objectMapper.writeValueAsString(collectedMessages));
            collectedUpdateRepository.save(update);
        } catch (JsonProcessingException e) {
            log.error("Could not store update {}", collectedMessages);
        }
    }

    public void sendActualUpdates() {
        collectedUpdateRepository.findByActualTillAfter(OffsetDateTime.now())
                                 .forEach(this::sendUpdateToUnreachedRooms);
    }

    private void sendUpdateToUnreachedRooms(CollectedUpdate update) {
        if (Strings.isNullOrEmpty(update.getMessagesJson())) {
            return;
        }
        try {
            var collectedMessages = objectMapper.readValue(update.getMessagesJson(), CollectedMessages.class);
            var messageLanguages = collectedMessages.getMessages()
                                                    .stream()
                                                    .map(CollectedMessage::getLanguage)
                                                    .collect(Collectors.toSet());
            var unreachedRooms = roomRepository.findUnreachedChats(update.getId());
            if (unreachedRooms.isEmpty()) {
                return;
            }
            unreachedRooms.stream()
                          .filter(room -> room.getPostedAt() == null ||
                                          room.getPostedAt()
                                              .plusMinutes(intervalPerMessagePerChatMinutes)
                                              .isBefore(OffsetDateTime.now())
                          )
                          .forEach(room -> sendUpdateToUnreachedRoom(
                                  room, update,
                                  getMessagesByLanguages(collectedMessages,
                                                         roomSettingsService
                                                                 .getSettingsFromString(room.getSettingsJson())
                                                                 .getLanguages())
                                   )
                          );
        } catch (JsonProcessingException e) {
            log.error("Could not deserialize JSON {}", update.getMessageJson(), e);
        }
    }

    private List<SupplierMessage> getMessagesByLanguages(CollectedMessages collectedMessages,
                                                         List<RoomLanguage> preferredLanguages) {
        var messageLanguages = collectedMessages.getMessages()
                                                .stream()
                                                .map(CollectedMessage::getLanguage)
                                                .collect(Collectors.toSet());
        return preferredLanguages.stream()
                                 .sequential()
                                 .filter(messageLanguages::contains)
                                 .findFirst()
                                 .stream()
                                 .flatMap(chosenLanguage ->
                                                  collectedMessages.getMessages()
                                                                   .stream()
                                                                   .filter(message -> message.getLanguage() ==
                                                                                      chosenLanguage)
                                 )
                                 .map(CollectedMessage::getMessage)
                                 .collect(Collectors.toList());
    }

    private void sendUpdateToUnreachedRoom(Room room, CollectedUpdate update, List<SupplierMessage> messages) {
        if (messages.isEmpty()) {
            return;
        }
        messages.forEach(message -> sendMessage(room, message));
        var distributedUpdate = new DistributedUpdate();
        distributedUpdate.setRoom(room);
        distributedUpdate.setCollectedUpdate(update);
        distributedUpdateRepository.save(distributedUpdate);
        room.setPostedAt(OffsetDateTime.now());
        roomRepository.save(room);
    }

    private void sendMessage(Room room, SupplierMessage message) { //TODO visitor?
        switch (room.getProvider()) {
            case TELEGRAM:
                try {
                    telegramBot.sendSupplierMessage(message, room.getProviderRoomKey());
                } catch (TelegramApiException e) {
                    log.error("Could not send Telegram message, skipped", e); //TODO skipped?
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown provider");
        }
    }
}
