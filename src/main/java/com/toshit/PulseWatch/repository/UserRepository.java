package com.toshit.PulseWatch.repository;

import com.toshit.PulseWatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);   //all of em by email
    boolean existsByEmail(String email);    //count
}