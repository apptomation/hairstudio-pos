package com.salonflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewAttributes {

    @Value("${app.pos-base-url:http://localhost:8080}")
    private String posBaseUrl;

    @ModelAttribute("posBaseUrl")
    public String posBaseUrl() {
        return posBaseUrl;
    }
}
