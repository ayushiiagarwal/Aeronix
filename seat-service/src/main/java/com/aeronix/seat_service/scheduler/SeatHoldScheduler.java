package com.aeronix.seat_service.scheduler;

import com.aeronix.seat_service.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatHoldScheduler {

    private final SeatService seatService;

    // Runs every 2 minutes — releases seats held > 15 mins without payment
    @Scheduled(cron = "${seat.hold.scheduler.cron}")
    public void releaseExpiredHolds() {
        log.info("Running seat hold expiry check...");
        seatService.releaseExpiredHolds();
    }
}