package com.themuler.reasonance.repository;

import com.themuler.reasonance.entity.Edge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EdgeRepository extends JpaRepository<Edge, UUID> {
    List<Edge> findByFromId(UUID fromId);
    List<Edge> findByToId(UUID toId);
    List<Edge> findByFromSpaceId(UUID spaceId);
    void deleteByFromSpaceId(UUID spaceId);
    void deleteByToSpaceId(UUID spaceId);
}
