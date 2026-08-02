package com.bsplay.physicalcard.application;

import java.time.Instant;
import java.util.UUID;

public record PhysicalCardImportReport(
        UUID id,
        String fileName,
        String checksum,
        String importVersion,
        int totalRows,
        int validRows,
        int invalidRows,
        String status,
        Instant createdAt,
        Instant completedAt
) {}
