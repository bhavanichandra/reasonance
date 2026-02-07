package com.themuler.reasonance.repository;

import com.themuler.reasonance.dto.FlatBoardView;
import com.themuler.reasonance.entity.Node;
import com.themuler.reasonance.entity.NodeType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NodeRepository extends JpaRepository<Node, UUID> {
    List<Node> findBySpaceId(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
    List<Node> findBySpaceIdOrderByGeneratedTurnAsc(UUID spaceId);
    List<Node> findBySpaceIdAndTypeOrderByGeneratedTurnAsc(UUID spaceId, NodeType type);

    List<Node> findBySpaceIdAndTypeAndState(UUID spaceId, NodeType type, String state);

    List<Node> findBySpaceIdAndTypeAndGeneratedTurnLessThanEqual(UUID spaceId, NodeType type, Integer generatedTurn);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT n FROM Node n WHERE n.id = :id")
    Optional<Node> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
                SELECT new com.themuler.reasonance.dto.FlatBoardView(
                    c.id, c.content, c.summary, c.generatedTurn,
                    w.id, w.content, w.summary, w.generatedTurn,
                    e.id, e.content, e.summary, e.generatedTurn,
                    nc.id, nc.content, nc.summary, nc.generatedTurn
                )
                FROM Node c
                LEFT JOIN Edge ecw ON ecw.from = c AND ecw.relation = 'INSPIRED'
                LEFT JOIN Node w ON ecw.to = w AND w.type = 'WHAT_IF'
                LEFT JOIN Edge ewe ON ewe.to = w AND ewe.relation = 'SUPPORTED_BY'
                LEFT JOIN Node e ON ewe.from = e AND e.type = 'EVIDENCE'
                LEFT JOIN Edge encw ON encw.to = w AND encw.relation = 'INFLUENCED_BY'
                LEFT JOIN Node nc ON encw.from = nc AND nc.type = 'CONCLUSION'
                WHERE c.space.id = :spaceId AND c.type = 'CONCLUSION'
            """)
    List<FlatBoardView> findBoardTree(@Param("spaceId") UUID spaceId);

    @Query("""
        SELECT e FROM Node e
        WHERE e.space.id = :spaceId
        AND e.type = 'EVIDENCE'
        AND NOT EXISTS (
            SELECT 1 FROM Edge edge
            WHERE edge.from = e
            AND edge.relation IN ('SUPPORTED_BY', 'SUPPORTED_CONCLUSION')
        )
    """)
    List<Node> findUnlinkedEvidences(@Param("spaceId") UUID spaceId);

    @Query("""
        SELECT n.id, t
        FROM Node n
        JOIN n.tags t
        WHERE n.space.id = :spaceId
        """)
    List<Object[]> findTagsBySpaceId(@Param("spaceId") UUID spaceId);

    @Query("""
        SELECT e.from FROM Edge e
        WHERE e.to.id = :whatIfId AND e.relation = 'INSPIRED'
        """)
    Optional<Node> findInsightByWhatIfId(@Param("whatIfId") UUID whatIfId);

    @Query("""
        SELECT e.from FROM Edge e
        WHERE e.to.id = :whatIfId AND e.relation = 'SUPPORTED_BY'
        """)
    List<Node> findEvidencesByWhatIfId(@Param("whatIfId") UUID whatIfId);
}
