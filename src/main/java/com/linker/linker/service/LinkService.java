package com.linker.linker.service;

import com.linker.linker.dto.request.LinkDtoRequest;
import com.linker.linker.entity.Link;
import com.linker.linker.entity.User;
import com.linker.linker.exception.LinkNotFoundException;
import com.linker.linker.handler.UrlHashGenerator;
import com.linker.linker.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkRepository linkRepository;

    public String createNewLink(Link mappedLink) {
        String newLinkHash = UrlHashGenerator.generateBase62Id();

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        mappedLink.setNewUrl(newLinkHash);
        mappedLink.setUser(user);

        this.linkRepository.save(mappedLink);

        return newLinkHash;
    }

    public Link update(Long id, LinkDtoRequest request) {
        Link link = this.linkRepository.findById(id)
                .orElseThrow(() -> new LinkNotFoundException("Link not found"));

        link.setOldUrl(request.getOldUrl());
        link.setStatus(request.getStatus() == null ? link.getStatus() : request.getStatus());
        link.setTimeToLeave(request.getTimeToLeave());

        return this.linkRepository.save(link);
    }

    public Page<Link> getAll(Pageable pageable) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return this.linkRepository.findByUser(user, pageable);
    }
}
