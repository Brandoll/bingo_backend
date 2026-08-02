package com.bsplay.physicalcard.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhysicalCardJpaRepository extends JpaRepository<PhysicalCardEntity, UUID> {
    Optional<PhysicalCardEntity> findByExternalIdIgnoreCase(String externalId);
}
