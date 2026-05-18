package com.aeronix.notification_service.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account.sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token:}")
    private String twilioAuthToken;

    @Value("${twilio.from.number:}")
    private String twilioFromNumber;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    @Async
    public void sendSms(String to, String message) {
        if (!twilioEnabled) {
            log.info("SMS (SIMULATED) to {}: {}", to, message);
            return;
        }

        try {
             Twilio.init(twilioAccountSid, twilioAuthToken);
             Message.creator(new PhoneNumber(to),
                             new PhoneNumber(twilioFromNumber),
                             message).create();
            log.info("SMS sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage());
        }
    }

    public String buildBookingConfirmationSms(String pnrCode, String flightNumber,
                                              String origin, String destination,
                                              String departureTime) {
        return String.format(
                "Aeronix: Booking Confirmed! PNR: %s | Flight: %s | " +
                        "%s->%s | Dep: %s | Web check-in opens 24h before departure.",
                pnrCode, flightNumber, origin, destination, departureTime);
    }

    public String buildCheckinReminderSms(String pnrCode, String flightNumber) {
        return String.format(
                "Aeronix: Check-in open for Flight %s (PNR: %s). " +
                        "Check in at aeronix.com/checkin. Closes 1h before departure.",
                flightNumber, pnrCode);
    }

    public String buildFlightDelaySms(String flightNumber, int delayMinutes) {
        return String.format(
                "Aeronix ALERT: Flight %s delayed by %d min. " +
                        "Check app for updated departure time.",
                flightNumber, delayMinutes);
    }

    public String buildGateChangeSms(String flightNumber, String newGate) {
        return String.format(
                "Aeronix ALERT: Gate changed for Flight %s. New Gate: %s. " +
                        "Please proceed accordingly.",
                flightNumber, newGate);
    }

    public String buildCancellationSms(String pnrCode) {
        return String.format(
                "Aeronix: Booking PNR %s has been cancelled. " +
                        "Refund will be processed in 5-7 working days.",
                pnrCode);
    }
}