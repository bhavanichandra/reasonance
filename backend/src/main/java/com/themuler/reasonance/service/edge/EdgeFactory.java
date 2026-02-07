package com.themuler.reasonance.service.edge;

import com.themuler.reasonance.entity.Edge;
import com.themuler.reasonance.entity.Node;
import com.themuler.reasonance.repository.EdgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EdgeFactory {

    private final EdgeRepository edgeRepository;

    @Transactional
    public void linkInfluencedBy(Node from, Node whatIf) {
        createEdge(from, whatIf, "INFLUENCED_BY");
    }

    @Transactional
    public void linkInspired(Node conclusion, Node whatIf) {
        createEdge(conclusion, whatIf, "INSPIRED");
    }

    @Transactional
    public void linkSupportedBy(Node evidence, Node whatIf) {
        createEdge(evidence, whatIf, "SUPPORTED_BY");
    }

    @Transactional
    public void linkSupportedConclusion(Node evidence, Node conclusion) {
        createEdge(evidence, conclusion, "SUPPORTED_CONCLUSION");
    }

    @Transactional
    public void linkDerivedFrom(Node finalNode, Node whatIf) {
        createEdge(finalNode, whatIf, "DERIVED_FROM");
    }


    protected void createEdge(Node from, Node to, String relation) {
        try {
            Edge edge = new Edge();
            edge.setFrom(from);
            edge.setTo(to);
            edge.setRelation(relation);
            edgeRepository.save(edge);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate edge ignored: {} -> {} ({})", from.getId(), to.getId(), relation);
        }
    }
}
