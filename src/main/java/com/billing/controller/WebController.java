package com.billing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    /**
     * Serve the login page from static resources.
     * NOTE: Spring Security handles /login itself for form submission.
     * We only handle GET /login to serve the HTML.
     */
    @GetMapping("/login")
    public String loginPage() {
        return "forward:/login.html";
    }

    /**
     * Serve the SPA for all app routes (authenticated users).
     */
    @GetMapping({"/", "/dashboard", "/billing", "/products", "/customers", "/reports"})
    public String spa() {
        return "forward:/index.html";
    }
}
