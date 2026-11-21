package com.itsm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.service.AccountService;
import com.itsm.service.classification.CategorySuggestion;
import com.itsm.service.classification.ClassificationService;
import com.itsm.service.classification.ClassifierStats;

/** Category suggestions for reporters, and classifier management for administrators. */
@RestController
public class ClassifierController {

    private final ClassificationService classificationService;
    private final AccountService accounts;

    public ClassifierController(ClassificationService classificationService, AccountService accounts) {
        this.classificationService = classificationService;
        this.accounts = accounts;
    }

    /** Suggest a category while the report is being written. */
    @PostMapping("/api/incidents/classify")
    public CategorySuggestion classify(@Valid @RequestBody ClassifyRequest request) {
        accounts.requireCurrentAccount();
        return classificationService.suggest(request.title(), request.description());
    }

    @GetMapping("/api/classifier")
    public ClassifierStats stats() {
        return classificationService.stats();
    }

    @PutMapping("/api/classifier/settings")
    public ClassifierStats configure(@Valid @RequestBody Settings settings) {
        classificationService.configure(settings.autoCorrect(), settings.threshold());
        return classificationService.stats();
    }

    @PostMapping("/api/classifier/retrain")
    public ClassifierStats retrain() {
        return classificationService.retrain();
    }

    public record ClassifyRequest(@NotBlank @Size(max = 255) String title, @Size(max = 4000) String description) {
    }

    public record Settings(@NotNull Boolean autoCorrect,
                           @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1.0") Double threshold) {
    }
}
