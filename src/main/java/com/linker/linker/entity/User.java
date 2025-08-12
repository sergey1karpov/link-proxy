package com.linker.linker.entity;

import com.linker.linker.entity.utils.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String password;

    private String avatar;

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL
    )
    private List<Link> links = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Role role;

    //Этот метод определяет какие права есть у пользователя
    //Например при использовании в контроллере с проверкой
    //
    //@PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
