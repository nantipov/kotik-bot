package org.nantipov.kotikbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.domain.entity.CollectedUpdate;
import org.nantipov.kotikbot.domain.entity.DistributedUpdate;
import org.nantipov.kotikbot.domain.entity.Room;
import org.nantipov.kotikbot.respository.CollectedUpdateRepository;
import org.nantipov.kotikbot.respository.DistributedUpdateRepository;
import org.nantipov.kotikbot.respository.RoomRepository;
import org.nantipov.kotikbot.service.telegram.TelegramBotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.OffsetDateTime;

@Slf4j
@Service
public class UpdatesDistributionService {

    private final CollectedUpdateRepository collectedUpdateRepository;
    private final RoomRepository roomRepository;
    private final DistributedUpdateRepository distributedUpdateRepository;
    private final ObjectMapper objectMapper;
    private final TelegramBotService telegramBotService;
    private final int intervalPerMessagePerChatMinutes;

    public UpdatesDistributionService(CollectedUpdateRepository collectedUpdateRepository,
                                      RoomRepository roomRepository,
                                      DistributedUpdateRepository distributedUpdateRepository,
                                      ObjectMapper objectMapper,
                                      TelegramBotService telegramBotService,
                                      @Value("${kotik.interval-per-message-per-chat-minutes}")
                                              int intervalPerMessagePerChatMinutes) {
        this.collectedUpdateRepository = collectedUpdateRepository;
        this.roomRepository = roomRepository;
        this.distributedUpdateRepository = distributedUpdateRepository;
        this.objectMapper = objectMapper;
        this.telegramBotService = telegramBotService;
        this.intervalPerMessagePerChatMinutes = intervalPerMessagePerChatMinutes;
    }

    @Cacheable(value = "already-delivered", unless = "!#result")
    public boolean isAlreadyDelivered(String supplierId, String updateKey) {
        return collectedUpdateRepository.existsBySupplierAndUpdateKey(supplierId, updateKey);
    }

    public void storeUpdate(String supplierId, String updateKey, OffsetDateTime actualTill, SupplierMessage message) {
        try {
            log.info("Storing update {} / {}", supplierId, updateKey);
            var update = new CollectedUpdate();
            update.setSupplier(supplierId);
            update.setUpdateKey(updateKey);
            update.setActualTill(actualTill);
            update.setMessageJson(objectMapper.writeValueAsString(message));
            collectedUpdateRepository.save(update);
        } catch (JsonProcessingException e) {
            log.error("Could not store update {}", message);
        }
    }

    public void sendActualUpdates() {
        collectedUpdateRepository.findByActualTillAfter(OffsetDateTime.now())
                                 .forEach(this::sendUpdateToUnreachedChats);
    }

    private void sendUpdateToUnreachedChats(CollectedUpdate update) {
        var unreachedChats = roomRepository.findUnreachedChats(update.getId());
        if (unreachedChats.isEmpty()) {
            return;
        }
        try {
            var message = objectMapper.readValue(update.getMessageJson(), SupplierMessage.class);
            unreachedChats.stream()
                          .filter(chat -> chat.getPostedAt() == null ||
                                          chat.getPostedAt()
                                              .plusMinutes(intervalPerMessagePerChatMinutes)
                                              .isBefore(OffsetDateTime.now())
                          )
                          .forEach(room -> sendUpdateToUnreachedChat(room, update, message));
        } catch (JsonProcessingException e) {
            log.error("Could not deserialize JSON {}", update.getMessageJson(), e);
        }
    }

    private void sendUpdateToUnreachedChat(Room room, CollectedUpdate update, SupplierMessage message) {
        sendMessage(room, message);
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
                    telegramBotService.sendSupplierMessage(message, room.getProviderRoomKey());
                } catch (TelegramApiException e) {
                    log.error("Could not send Telegram message, skipped", e); //TODO skipped?
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown provider");
        }
    }
}
