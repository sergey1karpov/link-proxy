package com.linker.linker.mail;

import com.linker.linker.dto.mail.ManualPasswordChangeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendManualPasswordChangeMail {
    private final RabbitTemplate rabbitTemplate;

    public void sendToQueue(ManualPasswordChangeDto message) {
        rabbitTemplate.convertAndSend("sendManualChangeMail", message);
    }
}
