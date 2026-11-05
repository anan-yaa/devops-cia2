package com.ananyaa.cicd.controller;

import com.ananyaa.cicd.dto.TransactionRequest;
import com.ananyaa.cicd.dto.TransactionResponse;
import com.ananyaa.cicd.entity.TransactionType;
import com.ananyaa.cicd.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService service;

    public TransactionController(TransactionService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        log.debug("Creating transaction");
        return service.create(request);
    }

    @GetMapping
    public List<TransactionResponse> list(@RequestParam(required = false) TransactionType type,
                                          @RequestParam(required = false) String category) {
        return service.list(type, category);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
