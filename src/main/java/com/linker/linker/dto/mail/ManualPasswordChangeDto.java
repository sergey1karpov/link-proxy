package com.linker.linker.dto.mail;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ManualPasswordChangeDto implements Serializable {
    private String to;
    private String subject;
    private String body;
}
