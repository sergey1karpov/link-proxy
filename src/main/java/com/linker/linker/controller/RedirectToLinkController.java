package com.linker.linker.controller;

import com.linker.linker.entity.Link;
import com.linker.linker.entity.utils.Status;
import com.linker.linker.exception.LinkNotFoundException;
import com.linker.linker.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class RedirectToLinkController {
    private final LinkRepository linkRepository;

    @GetMapping("/cc/{hash}")
    public String goToLink(@PathVariable String hash, Model model) {
        Link link = this.linkRepository.findByHash(hash)
                .orElseThrow(() -> new LinkNotFoundException("Link not found"));

        if (link.getTimeToLeave() != null && link.getTimeToLeave().isBefore(LocalDateTime.now())) {
            return "error-400";
        }

        if(link.getStatus() == Status.PRIVATE) {
            model.addAttribute("hash", hash);
            return "private-link-form";
        }

        return "redirect:" + link.getOldUrl();
    }

    @PostMapping("/cc/{hash}/check")
    public String checkPrivateCode(@PathVariable String hash,
                                   @RequestParam String code,
                                   RedirectAttributes redirectAttributes) {
        Link link = linkRepository.findByHash(hash)
                .orElseThrow(() -> new LinkNotFoundException("Link not found"));

        if (link.getPrivateCode().equals(code)) {
            return "redirect:" + link.getOldUrl();
        }

        if (link.getTimeToLeave() != null && link.getTimeToLeave().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "Время действия ссылки истекло");
            return "redirect:/cc/" + hash;
        }

        redirectAttributes.addFlashAttribute("error", "Неверный код доступа");
        return "redirect:/cc/" + hash;
    }
}
