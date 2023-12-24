package dev.softawii.repository;

import dev.softawii.entity.DynamicConfig;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface DynamicConfigurationRepository  extends JpaRepository<DynamicConfig, Long> {

    @Query("""
            select dc from DynamicConfig dc
            where
                dc.id = 0
            """)
    Optional<DynamicConfig> find();
}
