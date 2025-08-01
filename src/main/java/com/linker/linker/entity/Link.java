package com.linker.linker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @ManyToMany(mappedBy = "links")
    private List<User> users = new ArrayList<>();

    enum Status {
        PUBLIC, PRIVATE
    }
}
