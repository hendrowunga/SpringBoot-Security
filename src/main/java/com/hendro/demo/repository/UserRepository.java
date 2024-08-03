package com.hendro.demo.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.hendro.demo.model.User;

@Repository

public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByEmail(String Email);

    Optional<User> finByVertificationCode(String vertificationCode);

}
