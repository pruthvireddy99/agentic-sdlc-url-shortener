package com.example.agenticurl.controller;

import com.example.agenticurl.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RedirectController {
    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{code:[A-Za-z0-9]{4,32}}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        var result = urlService.resolve(code, request.getHeader("User-Agent"), request.getHeader("Referer"));
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, result.location().toString()).build();
    }
}
