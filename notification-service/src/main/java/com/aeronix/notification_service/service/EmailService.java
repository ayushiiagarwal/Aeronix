package com.aeronix.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@skybooker.com}")
    private String fromEmail;

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("HTML email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }

    public String buildBookingConfirmationEmail(String passengerName, String pnrCode,
                                                String flightNumber, String origin,
                                                String destination, String departureTime,
                                                String seatNumber, Double totalFare) {
        return "<html><body style='font-family:Arial,sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;" +
                "border:1px solid #ddd;border-radius:8px;'>" +
                "<h2 style='color:#1a73e8;'>✈ Booking Confirmed — Aeronix</h2>" +
                "<p>Dear <strong>" + passengerName + "</strong>,</p>" +
                "<p>Your booking is confirmed! Here are your details:</p>" +
                "<table style='width:100%;border-collapse:collapse;'>" +
                buildRow("PNR Code", "<strong style='font-size:18px;color:#1a73e8;'>" + pnrCode + "</strong>") +
                buildRow("Flight", flightNumber) +
                buildRow("Route", origin + " → " + destination) +
                buildRow("Departure", departureTime) +
                buildRow("Seat", seatNumber) +
                buildRow("Total Fare", "INR " + String.format("%.2f", totalFare)) +
                "</table>" +
                "<p style='margin-top:20px;color:#666;'>Web check-in opens 24 hours before departure.</p>" +
                "<p style='color:#666;'>Thank you for flying with Aeronix!</p>" +
                "</div></body></html>";
    }

    public String buildCheckinReminderEmail(String passengerName, String pnrCode,
                                            String flightNumber, String departureTime) {
        return "<html><body style='font-family:Arial,sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;" +
                "border:1px solid #ddd;border-radius:8px;'>" +
                "<h2 style='color:#f9a825;'>⏰ Check-In Reminder — Aeronix</h2>" +
                "<p>Dear <strong>" + passengerName + "</strong>,</p>" +
                "<p>Web check-in is now open for your upcoming flight!</p>" +
                "<table style='width:100%;border-collapse:collapse;'>" +
                buildRow("PNR Code", pnrCode) +
                buildRow("Flight", flightNumber) +
                buildRow("Departure", departureTime) +
                "</table>" +
                "<p style='margin-top:20px;'>Check-in closes <strong>1 hour</strong> before departure.</p>" +
                "style='background:#1a73e8;color:white;padding:10px 20px;" +
                "text-decoration:none;border-radius:4px;'>Check In Now</a>" +
                "</div></body></html>";
    }

    public String buildFlightAlertEmail(String type, String flightNumber,
                                        String message, String newTime,
                                        String newGate) {
        String color = type.contains("DELAY") ? "#e53935" :
                type.contains("GATE")  ? "#f9a825" : "#e53935";
        String icon  = type.contains("DELAY") ? "⚠" :
                type.contains("GATE")  ? "🚪" : "❌";

        return "<html><body style='font-family:Arial,sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;" +
                "border:1px solid #ddd;border-radius:8px;'>" +
                "<h2 style='color:" + color + ";'>" + icon + " Flight Alert — Aeronix</h2>" +
                "<p>" + message + "</p>" +
                "<table style='width:100%;border-collapse:collapse;'>" +
                buildRow("Flight", flightNumber) +
                (newTime != null ? buildRow("New Departure", newTime) : "") +
                (newGate != null ? buildRow("New Gate", newGate) : "") +
                "</table>" +
                "<p style='margin-top:20px;color:#666;'>We apologise for any inconvenience.</p>" +
                "</div></body></html>";
    }

    public String buildCancellationEmail(String passengerName, String pnrCode,
                                         Double refundAmount) {
        return "<html><body style='font-family:Arial,sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;" +
                "border:1px solid #ddd;border-radius:8px;'>" +
                "<h2 style='color:#e53935;'>❌ Booking Cancelled — Aeronix</h2>" +
                "<p>Dear <strong>" + passengerName + "</strong>,</p>" +
                "<p>Your booking <strong>" + pnrCode + "</strong> has been cancelled.</p>" +
                (refundAmount != null && refundAmount > 0
                        ? "<p>Refund of <strong>INR " + String.format("%.2f", refundAmount) +
                          "</strong> will be credited in 5-7 working days.</p>"
                        : "<p>No refund applicable per cancellation policy.</p>") +
                "<p style='color:#666;'>Thank you for using Aeronix.</p>" +
                "</div></body></html>";
    }

    private String buildRow(String label, String value) {
        return "<tr>" +
                "<td style='padding:8px;border-bottom:1px solid #eee;" +
                "color:#666;width:40%;'>" + label + "</td>" +
                "<td style='padding:8px;border-bottom:1px solid #eee;" +
                "font-weight:bold;'>" + value + "</td>" +
                "</tr>";
    }
}