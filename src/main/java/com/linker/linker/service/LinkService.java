package com.linker.linker.service;

import com.linker.linker.entity.Link;
import com.linker.linker.entity.User;
import com.linker.linker.handler.UrlHashGenerator;
import com.linker.linker.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkRepository linkRepository;

    public String createNewLink(Link mappedLink) {
        String newLinkHash = UrlHashGenerator.generateBase62Id();

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        mappedLink.setNewUrl(newLinkHash);
        mappedLink.setUser(user);

        linkRepository.save(mappedLink);

        return newLinkHash;
    }
}
