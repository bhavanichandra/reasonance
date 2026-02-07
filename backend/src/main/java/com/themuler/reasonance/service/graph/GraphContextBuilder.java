package com.themuler.reasonance.service.graph;

import com.themuler.reasonance.entity.Node;
import com.themuler.reasonance.entity.NodeType;
import com.themuler.reasonance.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GraphContextBuilder {

    private final NodeRepository nodeRepository;

    @Transactional(readOnly = true)
    public String buildGraphContext(UUID spaceId) {
        StringBuilder contextBuilder = new StringBuilder();
        List<Node> currentTurnNodes = new ArrayList<>();
        final int[] currentTurn = {-1};

        for (Node node : nodeRepository.findBySpaceIdOrderByGeneratedTurnAsc(spaceId)) {
            if (node.getGeneratedTurn() != currentTurn[0]) {
                if (currentTurn[0] != -1) {
                    appendTurnContext(contextBuilder, currentTurn[0], currentTurnNodes);
                    currentTurnNodes.clear();
                }
                currentTurn[0] = node.getGeneratedTurn();
            }
            currentTurnNodes.add(node);
        }

        if (!currentTurnNodes.isEmpty()) {
            appendTurnContext(contextBuilder, currentTurn[0], currentTurnNodes);
        }

        return contextBuilder.toString();
    }

    private void appendTurnContext(StringBuilder sb, int turn, List<Node> nodes) {
        sb.append("Turn ").append(turn).append(":\n");

        if (turn == 1) {
            nodes.stream()
                    .filter(n -> n.getType() == NodeType.SOURCE_SUMMARY)
                    .findFirst()
                    .ifPresent(summary -> sb.append("Source Summary: ").append(summary.getContent()).append("\n"));

            List<String> conclusions = nodes.stream()
                    .filter(n -> n.getType() == NodeType.CONCLUSION)
                    .map(Node::getContent)
                    .toList();
            if (!conclusions.isEmpty()) {
                sb.append("Conclusions: ").append(String.join("; ", conclusions)).append("\n");
            }

            List<String> evidences = nodes.stream()
                    .filter(n -> n.getType() == NodeType.EVIDENCE)
                    .map(Node::getContent)
                    .toList();
            if (!evidences.isEmpty()) {
                sb.append("Evidences: ").append(String.join("; ", evidences)).append("\n");
            }
        } else {
            nodes.stream()
                    .filter(n -> n.getType() == NodeType.WHAT_IF)
                    .findFirst()
                    .ifPresent(whatIf -> sb.append("What If: ").append(whatIf.getContent()).append("\n"));

            List<String> derivedConclusions = nodes.stream()
                    .filter(n -> n.getType() == NodeType.CONCLUSION)
                    .map(Node::getContent)
                    .toList();
            if (!derivedConclusions.isEmpty()) {
                sb.append("Derived Conclusions: ").append(String.join("; ", derivedConclusions)).append("\n");
            }

            List<String> derivedEvidences = nodes.stream()
                    .filter(n -> n.getType() == NodeType.EVIDENCE)
                    .map(Node::getContent)
                    .toList();
            if (!derivedEvidences.isEmpty()) {
                sb.append("Derived Evidences: ").append(String.join("; ", derivedEvidences)).append("\n");
            }
        }
        sb.append("\n");
    }
}
