package com.themuler.reasonance.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "spaces")
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "current_turn", nullable = false)
    private Integer currentTurn = 1;

    @Column(name = "max_turns", nullable = false)
    private Integer maxTurns = 5;

    @Column(name = "finalized_at")
    private OffsetDateTime finalizedAt;

    @Column(name = "final_investigation_summary", columnDefinition = "TEXT")
    private String finalInvestigationSummary;

    @Column(name = "final_case_closed")
    private Boolean finalCaseClosed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceStatus status = SpaceStatus.NEW;
}
