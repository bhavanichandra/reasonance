package com.themuler.reasonance.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "edges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"from_id", "to_id", "relation"})
})
public class Edge {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_id", nullable = false)
    private Node from;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_id", nullable = false)
    private Node to;

    @Column(nullable = false)
    private String relation;
}
