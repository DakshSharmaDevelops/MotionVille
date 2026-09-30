package org.example.motionville.repo.account;

import org.example.motionville.entity.account.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}
