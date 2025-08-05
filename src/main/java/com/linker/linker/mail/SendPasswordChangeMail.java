package com.linker.linker.mail;

import com.linker.linker.dto.mail.PasswordChangeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendPasswordChangeMail {
    private final RabbitTemplate rabbitTemplate;

    public void sendToQueue(PasswordChangeDto message) {
        rabbitTemplate.convertAndSend("sendManualChangeMail", message);
    }
}
