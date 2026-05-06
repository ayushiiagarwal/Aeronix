package com.aeronix.auth_service.repository;

import com.aeronix.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRole(User.Role role);

    Optional<User> findByPhone(String phone);

    Optional<User> findByPassportNumber(String passportNumber);

    void deleteByUserId(Integer userId);
}