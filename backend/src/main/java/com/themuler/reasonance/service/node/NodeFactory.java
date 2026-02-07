package com.themuler.reasonance.service.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.themuler.reasonance.entity.ConclusionState;
import com.themuler.reasonance.entity.EvidenceState;
import com.themuler.reasonance.entity.Node;
import com.themuler.reasonance.entity.NodeType;
import com.themuler.reasonance.entity.Space;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class NodeFactory {

    public Optional<Node> createNode(JsonNode json, NodeType type, int generatedTurn, Space space,
                                     String[] contentKeys, String[] summaryKeys) {
        if (json == null || json.isMissingNode() || json.isNull()) {
            return Optional.empty();
        }

        String content = null;
        String summary = null;
        Set<String> tags = new HashSet<>();

        if (json.isObject()) {
            content = firstText(json, contentKeys);
            summary = firstText(json, summaryKeys);

            // Fallback for summary if content exists but summary doesn't
            if (content != null && !content.isBlank() && (summary == null || summary.isBlank())) {
                summary = content;
            }

            // Extract tags
            JsonNode tagsNode = json.path("tags");
            if (tagsNode.isArray()) {
                for (JsonNode tag : tagsNode) {
                    String tagText = tag.asText();
                    if (tagText != null && !tagText.isBlank()) {
                        tags.add(tagText);
                    }
                }
            }
        } else {
            String text = json.asText(null);
            if (text != null && !text.isBlank()) {
                content = text;
                summary = text;
            }
        }

        if (content == null || content.isBlank()) {
            log.warn("Skipping node creation for type {} because content is blank. JSON: {}", type, json);
            return Optional.empty();
        }

        Node node = new Node();
        node.setSpace(space);
        node.setType(type);
        node.setGeneratedTurn(generatedTurn);
        node.setContent(content);
        node.setSummary(summary);
        node.setTags(tags);
        if (type == NodeType.CONCLUSION) {
            node.setState(ConclusionState.AVAILABLE.name());
            node.setLocked(false);
        } else if (type == NodeType.EVIDENCE) {
            node.setState(EvidenceState.AVAILABLE.name());
            node.setLocked(false);
        }

        return Optional.of(node);
    }

    private String firstText(JsonNode obj, String[] keys) {
        if (keys == null) return null;
        for (String key : keys) {
            JsonNode v = obj.path(key);
            if (!v.isMissingNode() && !v.isNull()) {
                String text = v.asText(null);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }
}
