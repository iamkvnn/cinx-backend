package com.cinx.notification.device.application;

import com.cinx.notification.device.application.command.RegisterDeviceTokenCommand;
import com.cinx.notification.device.domain.UserDeviceToken;
import com.cinx.notification.device.domain.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTokenService implements IDeviceTokenService {
    private final UserDeviceTokenRepository repository;

    @Override
    @Transactional
    public void register(String userId, RegisterDeviceTokenCommand command) {
        UserDeviceToken token = repository.findByFcmToken(command.fcmToken())
                .orElseGet(() -> UserDeviceToken.forToken(command.fcmToken()));
        token.assignTo(userId, command.deviceInfo());
        repository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findTokens(String userId) {
        return repository.findByUserId(userId).stream()
                .map(UserDeviceToken::getFcmToken)
                .toList();
    }
}
