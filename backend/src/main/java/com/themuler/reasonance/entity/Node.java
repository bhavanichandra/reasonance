package com.themuler.reasonance.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "nodes")
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "generated_turn", nullable = false)
    private Integer generatedTurn;

    @Column(name = "state")
    private String state;

    @Column(name = "completed_turn")
    private Integer completedTurn;

    @Column(name = "used_in_turn")
    private Integer usedInTurn;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(name = "confidence")
    private String confidence;

    @Column(name = "relevance")
    private String relevance;

    @Column(name = "reasoning_type")
    private String reasoningType;

    @Column(name = "references_turns", columnDefinition = "TEXT")
    private String referencesTurns;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "node_tags", joinColumns = @JoinColumn(name = "node_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private Space space;
}
