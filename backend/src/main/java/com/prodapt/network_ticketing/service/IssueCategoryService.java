package com.prodapt.network_ticketing.service;

import com.prodapt.network_ticketing.entity.IssueCategory;
import com.prodapt.network_ticketing.repository.IssueCategoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class IssueCategoryService {

    private final IssueCategoryRepository repository;

    public IssueCategoryService(IssueCategoryRepository repository) {
        this.repository = repository;
    }

    public List<IssueCategory> findAll() {
        return repository.findAll();
    }

    public IssueCategory save(IssueCategory category) {
        return repository.save(category);
    }

    public IssueCategory update(Long id, IssueCategory updated) {
        return repository.findById(id).map(existing -> {
            existing.setCategoryCode(updated.getCategoryCode());
            existing.setCategoryName(updated.getCategoryName());
            existing.setDescription(updated.getDescription());
            existing.setSlaHours(updated.getSlaHours());
            existing.setIsActive(updated.getIsActive());
            return repository.save(existing);
        }).orElseThrow(() -> new RuntimeException("IssueCategory not found"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}