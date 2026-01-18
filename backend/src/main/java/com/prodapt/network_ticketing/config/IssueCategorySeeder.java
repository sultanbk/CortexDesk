package com.prodapt.network_ticketing.config;

import com.prodapt.network_ticketing.entity.IssueCategory;
import com.prodapt.network_ticketing.repository.IssueCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class IssueCategorySeeder implements CommandLineRunner {

    private final IssueCategoryRepository repository;

    public IssueCategorySeeder(IssueCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        // desired seed categories (code -> [name, desc, slaHours])
        Map<String, Object[]> seed = new LinkedHashMap<>();
        seed.put("NET_OUTAGE", new Object[]{"Network Outage", "Complete loss of network connectivity or service down.", 4});
        seed.put("SLOW_PERFORMANCE", new Object[]{"Slow Performance", "Intermittent slowness or degraded performance.", 8});
        seed.put("AUTH_ISSUE", new Object[]{"Authentication Issue", "Users unable to login or authenticate.", 8});
        seed.put("HARDWARE_FAILURE", new Object[]{"Hardware Failure", "Physical device failure (switch/router/server).", 24});
        seed.put("SOFTWARE_BUG", new Object[]{"Application Bug", "Functional bug or unexpected application error.", 48});
        seed.put("CHANGE_REQUEST", new Object[]{"Change Request", "Request for configuration or approved change.", 72});
        seed.put("ACCESS_REQUEST", new Object[]{"Access Request", "Request for account or resource access.", 24});
        seed.put("BILLING_QUERY", new Object[]{"Billing / Account", "Billing related questions or account reconciliation.", 48});

        // existing codes in DB
        List<IssueCategory> existing = repository.findAll();
        Set<String> existingCodes = new HashSet<>();
        for (IssueCategory c : existing) {
            if (c.getCategoryCode() != null) existingCodes.add(c.getCategoryCode().toUpperCase());
        }

        List<IssueCategory> toSave = new ArrayList<>();
        for (Map.Entry<String, Object[]> e : seed.entrySet()) {
            String code = e.getKey();
            if (existingCodes.contains(code)) continue;
            Object[] info = e.getValue();
            IssueCategory cat = new IssueCategory();
            cat.setCategoryCode(code);
            cat.setCategoryName((String) info[0]);
            cat.setDescription((String) info[1]);
            cat.setSlaHours((Integer) info[2]);
            cat.setIsActive(true);
            toSave.add(cat);
        }

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
            System.out.println("Seeded " + toSave.size() + " issue categories.");
        }
    }
}
