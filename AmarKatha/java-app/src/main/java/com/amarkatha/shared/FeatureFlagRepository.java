package com.amarkatha.shared;

import com.amarkatha.shared.domain.FeatureFlag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {

    Optional<FeatureFlag> findByName(String name);
}
