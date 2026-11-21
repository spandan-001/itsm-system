package com.itsm.service.sla;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.Priority;
import com.itsm.model.SlaPolicy;
import com.itsm.repository.SlaPolicyRepository;

/** Stores the default SLA policy for any priority that has none yet. */
@Component
public class SlaPolicyInitializer implements ApplicationRunner {

    private final SlaPolicyRepository slaPolicyRepository;

    public SlaPolicyInitializer(SlaPolicyRepository slaPolicyRepository) {
        this.slaPolicyRepository = slaPolicyRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Priority priority : Priority.values()) {
            if (slaPolicyRepository.findByPriority(priority).isEmpty()) {
                slaPolicyRepository.save(SlaPolicy.defaultFor(priority));
            }
        }
    }
}
