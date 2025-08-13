package com.linker.linker.entity;

import com.linker.linker.entity.utils.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "links")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "old_url")
    private String oldUrl;

    @Column(name = "new_url")
    private String newUrl;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "time_to_leave")
    private LocalDateTime timeToLeave;

    @Column(name = "private_code")
    private String privateCode;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
