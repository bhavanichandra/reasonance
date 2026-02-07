package com.themuler.reasonance.service.graph;

import com.themuler.reasonance.entity.Node;
import com.themuler.reasonance.repository.NodeRepository;
import com.themuler.reasonance.service.edge.EdgeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GraphTransaction {

    private final NodeRepository nodeRepository;
    private final EdgeFactory edgeFactory;
    private final List<Node> pendingNodes = new ArrayList<>();
    private final List<Runnable> pendingEdges = new ArrayList<>();

    public GraphTransaction addNode(Node node) {
        pendingNodes.add(node);
        return this;
    }

    public GraphTransaction addNodes(List<Node> nodes) {
        pendingNodes.addAll(nodes);
        return this;
    }

    public GraphTransaction linkInfluencedBy(Node from, Node whatIf) {
        pendingEdges.add(() -> edgeFactory.linkInfluencedBy(from, whatIf));
        return this;
    }

    public GraphTransaction linkInspired(Node conclusion, Node whatIf) {
        pendingEdges.add(() -> edgeFactory.linkInspired(conclusion, whatIf));
        return this;
    }

    public GraphTransaction linkSupportedBy(Node evidence, Node whatIf) {
        pendingEdges.add(() -> edgeFactory.linkSupportedBy(evidence, whatIf));
        return this;
    }

    public GraphTransaction linkSupportedConclusion(Node evidence, Node conclusion) {
        pendingEdges.add(() -> edgeFactory.linkSupportedConclusion(evidence, conclusion));
        return this;
    }

    public GraphTransaction linkDerivedFrom(Node finalNode, Node whatIf) {
        pendingEdges.add(() -> edgeFactory.linkDerivedFrom(finalNode, whatIf));
        return this;
    }

    public List<Node> commit() {
        List<Node> savedNodes = nodeRepository.saveAll(pendingNodes);
        // Execute edge creation after nodes are saved and have IDs
        pendingEdges.forEach(Runnable::run);
        return savedNodes;
    }
}
