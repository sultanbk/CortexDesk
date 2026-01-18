package com.prodapt.network_ticketing.controller;

import com.prodapt.network_ticketing.entity.IssueCategory;
import com.prodapt.network_ticketing.service.IssueCategoryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/issue-categories")
@CrossOrigin(origins = "http://localhost:5173")
public class IssueCategoryController {

    private final IssueCategoryService service;

    public IssueCategoryController(IssueCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<IssueCategory> getCategories() {
        return service.findAll();
    }

    @PostMapping
    public IssueCategory createCategory(@RequestBody IssueCategory category) {
        return service.save(category);
    }

    @PutMapping("/{id}")
    public IssueCategory updateCategory(@PathVariable Long id, @RequestBody IssueCategory category) {
        return service.update(id, category);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        service.delete(id);
    }
}