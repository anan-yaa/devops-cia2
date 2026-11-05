package com.ananyaa.cicd.controller;

import com.ananyaa.cicd.dto.DashboardResponse;
import com.ananyaa.cicd.dto.TransactionForm;
import com.ananyaa.cicd.dto.TransactionResponse;
import com.ananyaa.cicd.entity.TransactionType;
import com.ananyaa.cicd.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Controller
public class ExpenseTrackerPageController {
    private final TransactionService service;

    public ExpenseTrackerPageController(TransactionService service) { this.service = service; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardResponse dashboard = service.dashboard();
        model.addAttribute("dashboard", dashboard);
        return "dashboard";
    }

    @GetMapping("/transactions")
    public String transactions(@RequestParam(required = false) TransactionType type,
                              @RequestParam(required = false) String category,
                              Model model) {
        List<TransactionResponse> items = service.list(type, category).stream()
                .sorted(Comparator.comparing(TransactionResponse::date)
                        .thenComparing(TransactionResponse::id).reversed())
                .toList();
        List<String> categories = service.list(null, null).stream()
                .map(TransactionResponse::category).distinct().sorted().toList();
        model.addAttribute("transactions", items);
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("categories", categories);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedCategory", category);
        return "transactions/list";
    }

    @GetMapping("/transactions/new")
    public String newTransaction(Model model) {
        model.addAttribute("transactionForm", new TransactionForm());
        prepareForm(model, false, null);
        return "transactions/form";
    }

    @PostMapping("/transactions")
    public String create(@Valid @ModelAttribute("transactionForm") TransactionForm form,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, false, null);
            return "transactions/form";
        }
        service.create(form.toRequest());
        return "redirect:/transactions";
    }

    @GetMapping("/transactions/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("transactionForm", TransactionForm.from(service.get(id)));
        model.addAttribute("transactionId", id);
        prepareForm(model, true, id);
        return "transactions/form";
    }

    @PostMapping("/transactions/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("transactionForm") TransactionForm form,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("transactionId", id);
            prepareForm(model, true, id);
            return "transactions/form";
        }
        service.update(id, form.toRequest());
        return "redirect:/transactions";
    }

    @PostMapping("/transactions/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/transactions";
    }

    private void prepareForm(Model model, boolean editing, Long id) {
        model.addAttribute("editing", editing);
        model.addAttribute("transactionTypes", Arrays.asList(TransactionType.values()));
        model.addAttribute("formAction", editing ? "/transactions/" + id : "/transactions");
    }
}
