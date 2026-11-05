package com.ananyaa.cicd.controller;

import com.ananyaa.cicd.dto.DashboardResponse;
import com.ananyaa.cicd.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final TransactionService service;
    public DashboardController(TransactionService service) { this.service = service; }

    @GetMapping
    public DashboardResponse dashboard() { return service.dashboard(); }
}
