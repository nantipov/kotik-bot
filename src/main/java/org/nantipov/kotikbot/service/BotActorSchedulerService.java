package org.nantipov.kotikbot.service;

import lombok.extern.slf4j.Slf4j;
import org.nantipov.kotikbot.service.updatesupplier.UpdateSupplier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class BotActorSchedulerService {

    private final List<UpdateSupplier> updateSuppliers;
    private final UpdatesService updatesService;

    public BotActorSchedulerService(List<UpdateSupplier> updateSuppliers,
                                    UpdatesService updatesService) {
        this.updateSuppliers = updateSuppliers;
        this.updatesService = updatesService;
    }

    @Scheduled(fixedDelayString = "${kotik.updates-scheduler-interval}")
    public void act() {
        updateSuppliers.forEach(this::callSupplierDelivery);
        updatesService.sendActualUpdates();
    }

    private void callSupplierDelivery(UpdateSupplier updateSupplier) {
        try {
            log.info("Delivering updates with supplier '{}'", updateSupplier.getClass().getCanonicalName());
            updateSupplier.deliver();
        } catch (RuntimeException e) {
            log.error("Could not deliver updates using supplier '{}'", updateSupplier.getClass().getCanonicalName(), e);
        }
    }
}
