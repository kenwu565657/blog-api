package com.contentfarm.web.controller.multimedia;

import com.contentfarm.inputport.multimedia.web.IMultimediaWebDomainService;
import com.contentfarm.web.controller.configuration.ContentFarmCdnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/multimedia")
public class MultimediaQueryController {

    private final IMultimediaWebDomainService multimediaWebDomainService;
    private final ContentFarmCdnProperties cdnProperties;

    @GetMapping(value = "/image/{fileName}", produces = "image/png+jpeg")
    public ResponseEntity<?> getMultimediaImageByFileName(@PathVariable String fileName) {
        if (cdnProperties.hasMediaBaseUrl()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(cdnProperties.buildMediaObjectUrl(fileName)))
                    .build();
        }
        byte[] imageBytes = multimediaWebDomainService.downloadImageAsync(fileName).block();
        return ResponseEntity.ok(imageBytes);
    }
}
