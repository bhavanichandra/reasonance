package com.themuler.reasonance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.themuler.reasonance.core.FileStore;
import com.themuler.reasonance.dto.*;
import com.themuler.reasonance.entity.*;
import com.themuler.reasonance.exception.DomainException;
import com.themuler.reasonance.exception.ResourceNotFoundException;
import com.themuler.reasonance.repository.DocumentRepository;
import com.themuler.reasonance.repository.NodeRepository;
import com.themuler.reasonance.repository.SpaceRepository;
import com.themuler.reasonance.service.edge.EdgeFactory;
import com.themuler.reasonance.service.graph.GraphContextBuilder;
import com.themuler.reasonance.service.graph.GraphTransaction;
import com.themuler.reasonance.service.llm.LlmResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindPalaceDataService {

    private final SpaceRepository spaceRepository;
    private final NodeRepository nodeRepository;
    private final DocumentRepository documentRepository;
    private final FileStore fileStore;
    private final ObjectMapper objectMapper;
    private final EdgeFactory edgeFactory;
    private final LlmResponseParser llmResponseParser;
    private final GraphContextBuilder graphContextBuilder;

    private final ObjectProvider<GraphTransaction> graphTransactionProvider;

    public record InitializeData(Space space, List<String> documentContents) {
    }

    public record AnalyzeData(Space space, Node conclusionNode, String conclusionText, List<Node> evidenceNodes,
                              String evidenceText) {
    }

    @Transactional(readOnly = true)
    public InitializeData fetchInitializeData(UUID spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));

        List<Document> documents = documentRepository.findBySpaceId(spaceId);
        log.info("Found {} documents for space: {}", documents.size(), spaceId);

        List<String> documentContents = new ArrayList<>();
        for (Document doc : documents) {
            documentContents.add(fileStore.getObjectContent(doc.getKey()));
        }
        return new InitializeData(space, documentContents);
    }

    @Transactional
    public InitializeResponse saveInitializeResponse(Space spaceIgnored, String llmResponse) throws JsonProcessingException {
        Space space = spaceRepository.findByIdWithLock(spaceIgnored.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceIgnored.getId()));
        if (space.getCurrentTurn() == null || space.getCurrentTurn() < 1) {
            space.setCurrentTurn(1);
        }
        int turn = space.getCurrentTurn();

        JsonNode rootNode = objectMapper.readTree(llmResponse);
        ParsedInitializeResult parsed = llmResponseParser.parseInitialize(rootNode);

        List<Node> nodesToSave = new ArrayList<>();

        if (parsed.summary() != null && !parsed.summary().isBlank()) {
            nodesToSave.add(createSummaryNode(parsed.summary(), space, turn));
        }

        if (parsed.conclusions() != null) {
            for (ParsedInitializeResult.Conclusion conclusion : parsed.conclusions()) {
                Node node = createConclusionNode(
                        conclusion.conclusion_content(),
                        conclusion.conclusion_summary(),
                        conclusion.tags(),
                        conclusion.confidence(),
                        space,
                        turn
                );
                if (node != null) {
                    nodesToSave.add(node);
                }
            }
        }

        if (parsed.evidences() != null) {
            for (ParsedInitializeResult.Evidence evidence : parsed.evidences()) {
                Node node = createEvidenceNode(
                        evidence.evidence_content(),
                        evidence.evidence_summary(),
                        evidence.tags(),
                        evidence.relevance(),
                        space,
                        turn
                );
                if (node != null) {
                    nodesToSave.add(node);
                }
            }
        }

        List<Node> savedNodes = nodeRepository.saveAll(nodesToSave);
        log.info("Saved {} initial analysis nodes", savedNodes.size());

        List<UUID> evidenceIds = new ArrayList<>();
        List<UUID> conclusionIds = new ArrayList<>();

        for (Node node : savedNodes) {
            if (node.getType() == NodeType.EVIDENCE) {
                evidenceIds.add(node.getId());
            } else if (node.getType() == NodeType.CONCLUSION) {
                conclusionIds.add(node.getId());
            }
        }

        return new InitializeResponse(space.getId(), evidenceIds, conclusionIds);
    }

    @Transactional(readOnly = true)
    public AnalyzeData fetchAnalyzeData(UUID spaceId, AnalyzeRequest request) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));

        Node conclusionNode = nodeRepository.findById(UUID.fromString(request.conclusionId()))
                .orElseThrow(() -> new DomainException("INSIGHT_NOT_AVAILABLE", "Insight not available",
                        "Selected insight is not available for analysis."));
        if (conclusionNode.getType() != NodeType.CONCLUSION
                || !ConclusionState.AVAILABLE.name().equals(conclusionNode.getState())) {
            throw new DomainException("INSIGHT_NOT_AVAILABLE", "Insight not available",
                    "Selected insight is not available for analysis.");
        }
        String conclusionText = conclusionNode.getContent();

        List<String> evidenceTexts = new ArrayList<>();
        List<Node> evidenceNodes = new ArrayList<>();
        for (String id : request.evidenceIds()) {
            Node evidenceNode = nodeRepository.findById(UUID.fromString(id))
                    .orElseThrow(() -> new DomainException("EVIDENCE_NOT_AVAILABLE", "Evidence not available",
                            "Selected evidence is not available for analysis."));
            if (evidenceNode.getType() != NodeType.EVIDENCE
                    || !EvidenceState.AVAILABLE.name().equals(evidenceNode.getState())) {
                throw new DomainException("EVIDENCE_NOT_AVAILABLE", "Evidence not available",
                        "Selected evidence is not available for analysis.");
            }
            evidenceNodes.add(evidenceNode);
            evidenceTexts.add(evidenceNode.getContent());
        }
        String evidenceText = String.join("\n\n", evidenceTexts);

        return new AnalyzeData(space, conclusionNode, conclusionText, evidenceNodes, evidenceText);
    }

    public record AnalyzeSaveResult(int completedTurn, Integer nextTurn, CompletedSnapshotDto snapshot,
                                    boolean reachedMaxTurns) {
    }

    @Transactional
    public AnalyzeSaveResult saveAnalyzeResponse(UUID spaceId, AnalyzeRequest request, String llmResponse) throws JsonProcessingException {
        Space space = spaceRepository.findByIdWithLock(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));
        if (space.getStatus() == SpaceStatus.CLOSED) {
            throw new DomainException("SPACE_ALREADY_FINALIZED", "Space already finalized",
                    "Space " + spaceId + " is already closed.");
        }
        Integer requestTurn = request.turn();
        if (requestTurn == null || !requestTurn.equals(space.getCurrentTurn())) {
            throw new DomainException("INVALID_TURN", "Invalid turn",
                    "Analyze turn " + requestTurn + " does not match current turn " + space.getCurrentTurn());
        }
        if (space.getCurrentTurn() > space.getMaxTurns()) {
            throw new DomainException("MAX_TURNS_REACHED", "Max turns reached",
                    "Max turns reached for space " + spaceId);
        }
        if (request.evidenceIds() == null || request.evidenceIds().isEmpty()) {
            throw new DomainException("NO_EVIDENCE_SELECTED", "No evidence selected",
                    "At least one evidence must be selected.");
        }

        Node conclusionNode = nodeRepository.findByIdForUpdate(UUID.fromString(request.conclusionId()))
                .orElseThrow(() -> new DomainException("INSIGHT_NOT_AVAILABLE", "Insight not available",
                        "Selected insight is not available for analysis."));
        if (conclusionNode.getType() != NodeType.CONCLUSION
                || !ConclusionState.AVAILABLE.name().equals(conclusionNode.getState())) {
            throw new DomainException("INSIGHT_NOT_AVAILABLE", "Insight not available",
                    "Selected insight is not available for analysis.");
        }

        List<Node> evidenceNodes = new ArrayList<>();
        for (String id : request.evidenceIds()) {
            Node evidenceNode = nodeRepository.findByIdForUpdate(UUID.fromString(id))
                    .orElseThrow(() -> new DomainException("EVIDENCE_NOT_AVAILABLE", "Evidence not available",
                            "Selected evidence is not available for analysis."));
            if (evidenceNode.getType() != NodeType.EVIDENCE
                    || !EvidenceState.AVAILABLE.name().equals(evidenceNode.getState())) {
                throw new DomainException("EVIDENCE_NOT_AVAILABLE", "Evidence not available",
                        "Selected evidence is not available for analysis.");
            }
            evidenceNodes.add(evidenceNode);
        }

        int completedTurn = space.getCurrentTurn();
        int nextTurn = completedTurn + 1;

        JsonNode rootNode = objectMapper.readTree(llmResponse);
        ParsedAnalyzeResult parsed = llmResponseParser.parseAnalyze(rootNode);

        GraphTransaction tx = graphTransactionProvider.getObject();

        Node whatIfNode = null;
        if (parsed.what_if() != null) {
            whatIfNode = createWhatIfNode(parsed.what_if(), space, completedTurn);
        }
        if (whatIfNode != null) {
            tx.addNode(whatIfNode);
        }

        List<Node> newConclusions = new ArrayList<>();
        List<Node> newEvidences = new ArrayList<>();
        if (completedTurn < space.getMaxTurns()) {
            if (parsed.conclusions() != null) {
                for (ParsedAnalyzeResult.Conclusion conclusion : parsed.conclusions()) {
                    Node node = createConclusionNode(
                            conclusion.conclusion_content(),
                            conclusion.conclusion_summary(),
                            conclusion.tags(),
                            conclusion.confidence(),
                            space,
                            nextTurn
                    );
                    if (node != null) {
                        newConclusions.add(node);
                        tx.addNode(node);
                    }
                }
            }
            if (parsed.evidences() != null) {
                for (ParsedAnalyzeResult.Evidence evidence : parsed.evidences()) {
                    Node node = createEvidenceNode(
                            evidence.evidence_content(),
                            evidence.evidence_summary(),
                            evidence.tags(),
                            evidence.relevance(),
                            space,
                            nextTurn
                    );
                    if (node != null) {
                        newEvidences.add(node);
                        tx.addNode(node);
                    }
                }
            }
        }

        if (whatIfNode != null) {
            tx.linkInspired(conclusionNode, whatIfNode);
            for (Node evidenceNode : evidenceNodes) {
                tx.linkSupportedBy(evidenceNode, whatIfNode);
            }
            for (Node nc : newConclusions) {
                tx.linkInfluencedBy(nc, whatIfNode);
            }
            for (Node ne : newEvidences) {
                tx.linkInfluencedBy(ne, whatIfNode);
            }
        }
        for (Node evidenceNode : evidenceNodes) {
            tx.linkSupportedConclusion(evidenceNode, conclusionNode);
        }

        List<Node> savedNodes = tx.commit();
        log.info("Saved {} new nodes from analysis", savedNodes.size());

        conclusionNode.setState(ConclusionState.COMPLETED.name());
        conclusionNode.setCompletedTurn(completedTurn);
        conclusionNode.setLocked(true);
        for (Node evidenceNode : evidenceNodes) {
            evidenceNode.setState(EvidenceState.USED.name());
            evidenceNode.setUsedInTurn(completedTurn);
            evidenceNode.setLocked(true);
        }

        Integer nextTurnValue = null;
        boolean reachedMaxTurns = completedTurn >= space.getMaxTurns();
        if (!reachedMaxTurns) {
            space.setCurrentTurn(nextTurn);
            nextTurnValue = nextTurn;
        }

        CompletedSnapshotDto snapshot = new CompletedSnapshotDto(
                completedTurn,
                new InsightSnapshotDto(conclusionNode.getId().toString(),
                        conclusionNode.getSummary(),
                        conclusionNode.getContent(),
                        conclusionNode.getGeneratedTurn()),
                evidenceNodes.stream()
                        .map(evidenceNode -> new EvidenceSnapshotDto(evidenceNode.getId().toString(),
                                evidenceNode.getSummary(),
                                evidenceNode.getContent(),
                                evidenceNode.getGeneratedTurn()))
                        .toList(),
                whatIfNode == null ? null : new WhatIfSnapshotDto(
                        whatIfNode.getId().toString(),
                        whatIfNode.getSummary(),
                        whatIfNode.getContent(),
                        whatIfNode.getGeneratedTurn())
        );

        return new AnalyzeSaveResult(completedTurn, nextTurnValue, snapshot, reachedMaxTurns);
    }

    @Transactional
    public Node saveFinalizeResponse(UUID spaceId, String llmResponse) throws JsonProcessingException {
        Space space = spaceRepository.findByIdWithLock(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));
        int finalTurn = space.getCurrentTurn();

        JsonNode rootNode = objectMapper.readTree(llmResponse);
        ParsedFinalizeResult parsed = llmResponseParser.parseFinalize(rootNode);
        ParsedFinalizeResult.FinalConclusion finalConclusion = parsed.final_conclusion();
        Node finalNode = createFinalConclusionNode(finalConclusion, space, finalTurn);
        if (finalNode == null) {
            throw new RuntimeException("LLM response has 'final_conclusion' but content is missing or blank");
        }
        storeFinalizationSummary(space, parsed);

        GraphTransaction tx = new GraphTransaction(nodeRepository, edgeFactory);
        tx.addNode(finalNode);

        List<Node> whatIfNodes = nodeRepository.findBySpaceIdAndTypeOrderByGeneratedTurnAsc(spaceId, NodeType.WHAT_IF);
        for (Node whatIf : whatIfNodes) {
            tx.linkDerivedFrom(finalNode, whatIf);
        }
        tx.commit();
        return finalNode;
    }

    @Transactional(readOnly = true)
    public String buildGraphContext(UUID spaceId) {
        return graphContextBuilder.buildGraphContext(spaceId);
    }

    @Transactional(readOnly = true)
    public String buildCompleteGraphContext(UUID spaceId) {
        return graphContextBuilder.buildGraphContext(spaceId);
    }

    @Transactional
    public void markSpaceFinalized(UUID spaceId) {
        Space space = spaceRepository.findByIdWithLock(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));
        space.setStatus(SpaceStatus.CLOSED);
        space.setFinalizedAt(java.time.OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public InvestigationSummaryDto fetchFinalInvestigationSummary(UUID spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));
        return parseInvestigationSummary(space.getFinalInvestigationSummary());
    }

    @Transactional(readOnly = true)
    public Boolean fetchFinalCaseClosed(UUID spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));
        return space.getFinalCaseClosed();
    }

    private Node createSummaryNode(String summary, Space space, int turn) {
        Node node = new Node();
        node.setType(NodeType.SOURCE_SUMMARY);
        node.setContent(summary);
        node.setSummary(summary);
        node.setTags(new HashSet<>());
        node.setSpace(space);
        node.setGeneratedTurn(turn);
        return node;
    }

    private Node createWhatIfNode(ParsedAnalyzeResult.WhatIf whatIf, Space space, int turn) {
        String content = whatIf.what_if_content();
        if (content == null || content.isBlank()) {
            return null;
        }

        Node node = new Node();
        node.setType(NodeType.WHAT_IF);
        node.setContent(content);
        node.setSummary(resolveSummary(content, whatIf.what_if_summary()));
        node.setTags(whatIf.tags() != null ? new HashSet<>(whatIf.tags()) : new HashSet<>());
        node.setSpace(space);
        node.setGeneratedTurn(turn);

        node.setConfidence(whatIf.confidence());
        node.setReasoningType(whatIf.reasoning_type());

        if (whatIf.references_turns() != null && !whatIf.references_turns().isEmpty()) {
            try {
                node.setReferencesTurns(objectMapper.writeValueAsString(whatIf.references_turns()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize references_turns", e);
            }
        }

        return node;
    }

    private Node createConclusionNode(String content, String summary, List<String> tags, String confidence,
                                      Space space, int turn) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Node node = new Node();
        node.setType(NodeType.CONCLUSION);
        node.setContent(content);
        node.setSummary(resolveSummary(content, summary));
        node.setTags(tags != null ? new HashSet<>(tags) : new HashSet<>());
        node.setSpace(space);
        node.setGeneratedTurn(turn);
        node.setConfidence(confidence);
        node.setState(ConclusionState.AVAILABLE.name());
        node.setLocked(false);
        return node;
    }

    private Node createEvidenceNode(String content, String summary, List<String> tags, String relevance,
                                    Space space, int turn) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Node node = new Node();
        node.setType(NodeType.EVIDENCE);
        node.setContent(content);
        node.setSummary(resolveSummary(content, summary));
        node.setTags(tags != null ? new HashSet<>(tags) : new HashSet<>());
        node.setSpace(space);
        node.setGeneratedTurn(turn);
        node.setRelevance(relevance);
        node.setState(EvidenceState.AVAILABLE.name());
        node.setLocked(false);
        return node;
    }

    private Node createFinalConclusionNode(ParsedFinalizeResult.FinalConclusion finalConclusion, Space space, int turn) {
        if (finalConclusion == null) {
            return null;
        }
        String content = finalConclusion.final_conclusion_content();
        if (content == null || content.isBlank()) {
            return null;
        }

        Node node = new Node();
        node.setType(NodeType.FINAL_CONCLUSION);
        node.setContent(content);
        node.setSummary(resolveSummary(content, finalConclusion.final_conclusion_summary()));
        node.setTags(finalConclusion.tags() != null ? new HashSet<>(finalConclusion.tags()) : new HashSet<>());
        node.setSpace(space);
        node.setGeneratedTurn(turn);
        node.setConfidence(finalConclusion.confidence());
        return node;
    }

    private String resolveSummary(String content, String summary) {
        if (summary == null || summary.isBlank()) {
            return content;
        }
        return summary;
    }

    private void storeFinalizationSummary(Space space, ParsedFinalizeResult parsed) {
        if (parsed == null) {
            return;
        }
        if (parsed.investigation_summary() != null) {
            try {
                space.setFinalInvestigationSummary(objectMapper.writeValueAsString(parsed.investigation_summary()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize investigation_summary", e);
            }
        }
        if (parsed.case_closed() != null) {
            space.setFinalCaseClosed(parsed.case_closed());
        }
    }

    private InvestigationSummaryDto parseInvestigationSummary(String summaryJson) {
        if (summaryJson == null || summaryJson.isBlank()) {
            return null;
        }
        try {
            ParsedFinalizeResult.InvestigationSummary summary =
                    objectMapper.readValue(summaryJson, ParsedFinalizeResult.InvestigationSummary.class);
            List<DecisiveMomentDto> decisiveMoments = null;
            if (summary.decisive_moments() != null) {
                decisiveMoments = summary.decisive_moments().stream()
                        .map(moment -> new DecisiveMomentDto(moment.turn(), moment.moment()))
                        .toList();
            }
            return new InvestigationSummaryDto(
                    summary.total_turns(),
                    summary.total_hypotheses_explored(),
                    summary.critical_evidence_count(),
                    summary.reasoning_path(),
                    decisiveMoments
            );
        } catch (Exception e) {
            log.warn("Failed to parse investigation_summary from storage", e);
            return null;
        }
    }
}
