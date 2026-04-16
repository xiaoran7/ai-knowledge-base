package com.ai.kb.repository;

import com.ai.kb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    //根据用户名查询用户，返回optional对象,避免NullPointerEeception
    Optional<User> findByUsername(String username);

    //根据邮箱查询用户
    Optional<User> findByEmail(String email);

    //判断用户名是否存在
    boolean existsByUsername(String username);

    //判断邮箱是否存在
    boolean existsByEmail(String email);
}
