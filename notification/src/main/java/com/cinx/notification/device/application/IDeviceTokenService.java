package com.cinx.notification.device.application;

import com.cinx.notification.device.application.command.RegisterDeviceTokenCommand;

import java.util.List;

public interface IDeviceTokenService {
    void register(String userId, RegisterDeviceTokenCommand command);
    List<String> findTokens(String userId);
}
