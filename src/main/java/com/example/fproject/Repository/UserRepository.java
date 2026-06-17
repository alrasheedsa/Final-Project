package com.example.fproject.Repository;

import com.example.fproject.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findUserById(Integer id);

    User findUserByEmail(String email);

    User findUserByPhone(String phone);

    Boolean existsUserByEmail(String email);

    Boolean existsUserByPhone(String phone);
}