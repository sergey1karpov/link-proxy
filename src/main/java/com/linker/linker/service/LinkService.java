package com.linker.linker.service;

import com.linker.linker.entity.Link;
import com.linker.linker.entity.User;
import com.linker.linker.handler.UrlHashGenerator;
import com.linker.linker.repository.LinkRepository;
import com.linker.linker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;

    public String createNewLink(Link mappedLink) {
        String newLink = "http://localhost:8080/cc/" + UrlHashGenerator.generateBase62Id();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> user = this.userRepository.findByUsername(userDetails.getUsername());

        mappedLink.setNewUrl(newLink);
        mappedLink.setUser(user.get());

        linkRepository.save(mappedLink);

        return newLink;
    }
}
