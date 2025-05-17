package com.example.demo.repository;

import com.example.demo.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {
    List<ChatGroup> findByMembersUsername(String username);
    
    @Query("SELECT g FROM ChatGroup g LEFT JOIN FETCH g.members WHERE g.id = :id")
    Optional<ChatGroup> findByIdWithMembers(@Param("id") Long id);
}
