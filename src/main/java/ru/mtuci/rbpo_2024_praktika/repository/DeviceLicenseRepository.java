package ru.mtuci.rbpo_2024_praktika.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mtuci.rbpo_2024_praktika.model.DeviceLicense;

@Repository
public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {
    boolean existsByLicenseIdAndDeviceId(Long licenseId, Long deviceId);
}