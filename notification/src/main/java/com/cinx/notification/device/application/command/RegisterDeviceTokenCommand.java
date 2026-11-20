package com.cinx.notification.device.application.command;

/** A device announces its push token; {@code deviceInfo} is a free-text label of the device. */
public record RegisterDeviceTokenCommand(String fcmToken, String deviceInfo) {
}
