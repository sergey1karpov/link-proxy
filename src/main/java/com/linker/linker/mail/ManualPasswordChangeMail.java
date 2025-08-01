package com.linker.linker.mail;

import com.linker.linker.dto.mail.ManualPasswordChangeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
@RequiredArgsConstructor
public class ManualPasswordChangeMail implements Serializable {
    private final MailSender mailSender;

    @RabbitListener(queues = "sendManualChangeMail")
    public void sendManualChangeMail(ManualPasswordChangeDto dto) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(dto.getTo());
        mail.setSubject(dto.getSubject());
        mail.setText(dto.getBody());
        mailSender.send(mail);
    }
}
