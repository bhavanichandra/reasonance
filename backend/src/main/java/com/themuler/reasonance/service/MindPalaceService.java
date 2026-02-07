package com.themuler.reasonance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.themuler.reasonance.core.FileStore;
import com.themuler.reasonance.core.ServicePrompt;
import com.themuler.reasonance.dto.*;
import com.themuler.reasonance.entity.*;
import com.themuler.reasonance.exception.DomainException;
import com.themuler.reasonance.exception.ResourceNotFoundException;
import com.themuler.reasonance.repository.DocumentRepository;
import com.themuler.reasonance.repository.EdgeRepository;
import com.themuler.reasonance.repository.NodeRepository;
import com.themuler.reasonance.repository.SpaceRepository;
import com.themuler.reasonance.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindPalaceService {

    private final SpaceRepository spaceRepository;
    private final NodeRepository nodeRepository;
    private final DocumentRepository documentRepository;
    private final EdgeRepository edgeRepository;
    private final FileStore fileStore;
    private final LlmService llmService;
    private final Faker faker = new Faker();
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final MindPalaceDataService dataService;

    @Transactional
    public CreateSpaceResponse createSpaceWithFiles(MultipartFile[] files) {
        log.info("Creating space with {} files", files.length);
        String spaceName = faker.space().galaxy() + " " + faker.ancient().titan();
        Space space = new Space();
        space.setName(spaceName);
        space.setStatus(SpaceStatus.NEW);
        Space savedSpace = spaceRepository.saveAndFlush(space);
        log.info("Space created with ID: {}", savedSpace.getId());

        List<Document> documents = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        for (MultipartFile file : files) {
            Document document = new Document();
            document.setSpace(savedSpace);
            document.setName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.txt");
            document.setKey("temp");
            document = documentRepository.save(document);

            String key = String.format("%s/%s/%s_%s", savedSpace.getId(), document.getId(), timestamp, document.getName());
            document.setKey(key);
            document = documentRepository.save(document);

            documents.add(document);
        }

        List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            Document document = documents.get(i);
            uploadFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    log.debug("Uploading file: {}", document.getName());
                    fileStore.putObject(document.getKey(), file.getInputStream(), file.getSize(), file.getContentType());
                    log.debug("Uploaded file: {}", document.getName());
                } catch (IOException e) {
                    log.error("Failed to upload file: {}", document.getName(), e);
                    throw new RuntimeException("Failed to upload file: " + document.getName(), e);
                }
            }, executorService));
        }

        CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();
        log.info("All files uploaded successfully for space: {}", savedSpace.getId());

        return new CreateSpaceResponse(savedSpace, documents);
    }

    public InitializeResponse initializeAnalysis(UUID spaceId) {
        log.info("Initializing analysis for space: {}", spaceId);

        // Fetch data (Transactional)
        MindPalaceDataService.InitializeData data = dataService.fetchInitializeData(spaceId);

        String combinedText = String.join("\n\n", data.documentContents());

        log.info("Generating initial analysis using LLM");
        String prompt = String.format(ServicePrompt.INITIAL_ANALYSIS_PROMPT.getPrompt(), combinedText);
        log.info("Prompt size for Turn 1: {} chars", prompt.length());

        // Generate (No Tx)
        String llmResponse = llmService.generate(ServicePrompt.INITIAL_ANALYSIS_PROMPT, combinedText);
        log.debug("LLM Response: {}", llmResponse);

        try {
            // Save result (Transactional)
            InitializeResponse response = dataService.saveInitializeResponse(data.space(), llmResponse);

            // Update status to IN_PROGRESS after initialization
            Space space = data.space();
            space.setStatus(SpaceStatus.IN_PROGRESS);
            spaceRepository.save(space);

            return response;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response", e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        log.info("Starting deep dive analysis for space: {}", request.spaceId());
        UUID spaceId = UUID.fromString(request.spaceId());

        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));
        if (space.getStatus() == SpaceStatus.CLOSED) {
            throw new DomainException("SPACE_ALREADY_FINALIZED",
                    "Space already finalized", "Space " + spaceId + " is already closed.");
        }
        if (space.getCurrentTurn() > space.getMaxTurns()) {
            throw new DomainException("MAX_TURNS_REACHED",
                    "Max turns reached", "Max turns reached for space " + spaceId);
        }
        if (request.turn() == null || !request.turn().equals(space.getCurrentTurn())) {
            throw new DomainException("INVALID_TURN",
                    "Invalid turn", "Analyze turn " + request.turn() + " does not match current turn " + space.getCurrentTurn());
        }
        if (request.evidenceIds() == null || request.evidenceIds().isEmpty()) {
            throw new DomainException("NO_EVIDENCE_SELECTED",
                    "No evidence selected", "At least one evidence must be selected.");
        }

        MindPalaceDataService.AnalyzeData data = dataService.fetchAnalyzeData(spaceId, request);

        int promptTurn = space.getCurrentTurn() + 1;
        log.info("Generating deep dive analysis using LLM with Graph Context for Turn {}", promptTurn);

        String graphContext = dataService.buildGraphContext(spaceId);
        String llmResponse = llmService.generate(ServicePrompt.DEEP_DIVE_PROMPT, promptTurn, graphContext,
                data.conclusionText(), data.evidenceText(), promptTurn);
        log.debug("LLM Response: {}", llmResponse);

        try {
            MindPalaceDataService.AnalyzeSaveResult saved = dataService.saveAnalyzeResponse(spaceId, request, llmResponse);

            boolean autoFinalized = false;
            String finalConclusionId = null;
            Integer nextTurn = saved.nextTurn();

            if (saved.reachedMaxTurns()) {
                String finalGraphContext = dataService.buildGraphContext(spaceId);
                String finalResponse = llmService.generate(ServicePrompt.FINAL_SYNTHESIS_PROMPT,
                        saved.completedTurn(), finalGraphContext);
                Node finalNode = dataService.saveFinalizeResponse(spaceId, finalResponse);
                dataService.markSpaceFinalized(spaceId);
                autoFinalized = true;
                finalConclusionId = finalNode.getId().toString();
                nextTurn = null;
            } else if (space.getStatus() == SpaceStatus.NEW) {
                space.setStatus(SpaceStatus.IN_PROGRESS);
                spaceRepository.save(space);
            }

            return new AnalyzeResponse(true, saved.completedTurn(), nextTurn, autoFinalized, saved.snapshot(), finalConclusionId);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response", e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    public FinalizeResponse finalizeSpace(UUID spaceId) {
        log.info("Finalizing space: {}", spaceId);

        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));
        if (space.getStatus() == SpaceStatus.CLOSED) {
            throw new DomainException("SPACE_ALREADY_FINALIZED",
                    "Space already finalized", "Space " + spaceId + " is already closed.");
        }
        int totalTurns = space.getCurrentTurn();

        String graphContext = dataService.buildCompleteGraphContext(spaceId);
        String fullPrompt = String.format(ServicePrompt.FINAL_SYNTHESIS_PROMPT.getPrompt(), totalTurns, graphContext);
        log.info("Prompt size for Finalization: {} chars", fullPrompt.length());

        log.info("Generating final conclusion using LLM after {} turns", totalTurns);
        // Generate (No Tx) - Pass total turns and graph context
        String llmResponse = llmService.generate(ServicePrompt.FINAL_SYNTHESIS_PROMPT, totalTurns, graphContext);
        log.debug("LLM Response: {}", llmResponse);

        try {
            // Save result (Transactional)
            Node node = dataService.saveFinalizeResponse(spaceId, llmResponse);

            dataService.markSpaceFinalized(spaceId);

            InvestigationSummaryDto investigationSummary = dataService.fetchFinalInvestigationSummary(spaceId);
            Boolean caseClosed = dataService.fetchFinalCaseClosed(spaceId);
            FinalConclusionDto finalConclusion = new FinalConclusionDto(
                    node.getId(),
                    node.getType(),
                    node.getContent(),
                    node.getSummary(),
                    node.getTags()
            );
            return new FinalizeResponse(finalConclusion, investigationSummary, caseClosed);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response", e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    @Transactional(readOnly = true)
    public BoardStateResponse getBoardState(UUID spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));

        int currentTurn = space.getCurrentTurn();
        boolean finalized = space.getStatus() == SpaceStatus.CLOSED;

        List<TurnDto> turns = new ArrayList<>();
        if (currentTurn > 0) {
            for (int turn = 1; turn <= currentTurn; turn++) {
                TurnStatus status = finalized
                        ? TurnStatus.COMPLETED
                        : (turn < currentTurn ? TurnStatus.COMPLETED : TurnStatus.IN_PROGRESS);
                turns.add(new TurnDto(turn, status));
            }
        }

        int completedThrough = finalized ? currentTurn : currentTurn - 1;
        Map<Integer, List<CompletedSnapshotDto>> completedSnapshotsByTurn = new HashMap<>();
        if (completedThrough >= 1) {
            List<Node> whatIfNodes = nodeRepository.findBySpaceIdAndTypeAndGeneratedTurnLessThanEqual(
                    spaceId, NodeType.WHAT_IF, completedThrough);
            for (Node whatIfNode : whatIfNodes) {
                int turn = whatIfNode.getGeneratedTurn();
                InsightSnapshotDto insightSnapshot = nodeRepository.findInsightByWhatIfId(whatIfNode.getId())
                        .map(this::toInsightSnapshot)
                        .orElse(null);
                List<EvidenceSnapshotDto> evidenceSnapshots = nodeRepository.findEvidencesByWhatIfId(whatIfNode.getId())
                        .stream()
                        .map(this::toEvidenceSnapshot)
                        .toList();
                CompletedSnapshotDto snapshot = new CompletedSnapshotDto(
                        turn,
                        insightSnapshot,
                        evidenceSnapshots,
                        new WhatIfSnapshotDto(whatIfNode.getId().toString(), whatIfNode.getSummary(),
                                whatIfNode.getContent(), whatIfNode.getGeneratedTurn())
                );
                completedSnapshotsByTurn.computeIfAbsent(turn, key -> new ArrayList<>()).add(snapshot);
            }
        }

        List<InsightDto> availableInsights = nodeRepository.findBySpaceIdAndTypeAndState(
                        spaceId, NodeType.CONCLUSION, ConclusionState.AVAILABLE.name())
                .stream()
                .map(this::toInsightDto)
                .toList();
        List<EvidenceDto> availableEvidences = nodeRepository.findBySpaceIdAndTypeAndState(
                        spaceId, NodeType.EVIDENCE, EvidenceState.AVAILABLE.name())
                .stream()
                .map(this::toEvidenceDto)
                .toList();

        InProgressDto inProgress = new InProgressDto(availableInsights, availableEvidences);

        return new BoardStateResponse(
                space.getId().toString(),
                currentTurn,
                space.getMaxTurns(),
                finalized,
                turns,
                completedSnapshotsByTurn,
                inProgress
        );
    }


    @Transactional(readOnly = true)
    public TreeResponse getNodeTree(UUID spaceId) {
        // 1. Fetch the main tree structure using the optimized SQL query
        List<FlatBoardView> flatViews = nodeRepository.findBoardTree(spaceId);

        // 2. Load all tags separately
        List<Object[]> tagRows = nodeRepository.findTagsBySpaceId(spaceId);

        // 3. Build a map: nodeId -> Set<String> tags
        Map<UUID, Set<String>> tagsMap = new HashMap<>();
        for (Object[] row : tagRows) {
            UUID nodeId = (UUID) row[0];
            String tag = (String) row[1];
            tagsMap.computeIfAbsent(nodeId, k -> new HashSet<>()).add(tag);
        }

        // 4. Group by Conclusion ID to reconstruct the tree
        Map<UUID, List<FlatBoardView>> groupedByConclusion = flatViews.stream()
                .collect(Collectors.groupingBy(FlatBoardView::cId));

        List<BoardNodeDto> boardNodes = new ArrayList<>();

        for (Map.Entry<UUID, List<FlatBoardView>> entry : groupedByConclusion.entrySet()) {
            FlatBoardView first = entry.getValue().get(0);

            BoardNodeDto boardNode = new BoardNodeDto();
            boardNode.setId(first.cId());
            boardNode.setType(NodeType.CONCLUSION);
            boardNode.setContent(first.cContent());
            boardNode.setSummary(first.cSummary());
            boardNode.setTurn(first.cTurn());
            boardNode.setTags(tagsMap.getOrDefault(first.cId(), Set.of()));
            boardNode.setEvidences(new ArrayList<>());
            boardNode.setInfluencedConclusions(new ArrayList<>());

            // Check if there is a linked What-If
            if (first.wId() != null) {
                boardNode.setLocked(true);

                NodeDto whatIfDto = new NodeDto();
                whatIfDto.setId(first.wId());
                whatIfDto.setType(NodeType.WHAT_IF);
                whatIfDto.setContent(first.wContent());
                whatIfDto.setSummary(first.wSummary());
                whatIfDto.setTurn(first.wTurn());
                whatIfDto.setTags(tagsMap.getOrDefault(first.wId(), Set.of()));
                boardNode.setWhatIf(whatIfDto);

                // Collect Evidences and Influenced Conclusions from the flat view
                // Use Sets to avoid duplicates if the join produces multiple rows per child
                Map<UUID, NodeDto> uniqueEvidences = new java.util.HashMap<>();
                Map<UUID, NodeDto> uniqueConclusions = new java.util.HashMap<>();

                for (FlatBoardView view : entry.getValue()) {
                    if (view.eId() != null) {
                        NodeDto dto = new NodeDto();
                        dto.setId(view.eId());
                        dto.setType(NodeType.EVIDENCE);
                        dto.setContent(view.eContent());
                        dto.setSummary(view.eSummary());
                        dto.setTurn(view.eTurn());
                        dto.setTags(tagsMap.getOrDefault(view.eId(), Set.of()));
                        uniqueEvidences.putIfAbsent(view.eId(), dto);
                    }
                    if (view.ncId() != null) {
                        NodeDto dto = new NodeDto();
                        dto.setId(view.ncId());
                        dto.setType(NodeType.CONCLUSION);
                        dto.setContent(view.ncContent());
                        dto.setSummary(view.ncSummary());
                        dto.setTurn(view.ncTurn());
                        dto.setTags(tagsMap.getOrDefault(view.ncId(), Set.of()));
                        uniqueConclusions.putIfAbsent(view.ncId(), dto);
                    }
                }
                boardNode.setEvidences(new ArrayList<>(uniqueEvidences.values()));
                boardNode.setInfluencedConclusions(new ArrayList<>(uniqueConclusions.values()));
            }
            boardNodes.add(boardNode);
        }

        // 3. Fetch unlinked evidence using the optimized SQL query
        List<Node> unlinkedEvidences = nodeRepository.findUnlinkedEvidences(spaceId);

        if (!unlinkedEvidences.isEmpty()) {
            BoardNodeDto unlinkedNodeContainer = new BoardNodeDto();
            unlinkedNodeContainer.setId(UUID.randomUUID());
            unlinkedNodeContainer.setType(NodeType.EVIDENCE);
            unlinkedNodeContainer.setContent("Unlinked Evidences");
            unlinkedNodeContainer.setTurn(0);
            unlinkedNodeContainer.setEvidences(unlinkedEvidences.stream()
                    .map(n -> {
                        NodeDto dto = new NodeDto();
                        dto.setId(n.getId());
                        dto.setType(n.getType());
                        dto.setContent(n.getContent());
                        dto.setSummary(n.getSummary());
                        dto.setTurn(n.getGeneratedTurn());
                        dto.setTags(n.getTags());
                        return dto;
                    })
                    .collect(Collectors.toList()));
            unlinkedNodeContainer.setInfluencedConclusions(new ArrayList<>());
            boardNodes.add(unlinkedNodeContainer);
        }

        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));

        // 4. Fetch Final Conclusion if exists
        FinalConclusionDto finalConclusionDto = null;
        List<Node> finalConclusions = nodeRepository.findBySpaceIdAndTypeOrderByGeneratedTurnAsc(spaceId, NodeType.FINAL_CONCLUSION);
        if (!finalConclusions.isEmpty()) {
            Node fc = finalConclusions.get(0);
            finalConclusionDto = new FinalConclusionDto(fc.getId(), fc.getType(), fc.getContent(), fc.getSummary(),
                    tagsMap.getOrDefault(fc.getId(), Set.of()));
        }

        InvestigationSummaryDto investigationSummary = dataService.fetchFinalInvestigationSummary(spaceId);
        Boolean caseClosed = dataService.fetchFinalCaseClosed(spaceId);

        return new TreeResponse(boardNodes, space.getCurrentTurn(), finalConclusionDto, investigationSummary, caseClosed);
    }


    @Transactional(readOnly = true)
    public List<SpaceDto> getAllSpaces() {
        return spaceRepository.findAll().stream()
                .map(space -> {
                    SpaceDto dto = new SpaceDto();
                    dto.setId(space.getId());
                    dto.setName(space.getName());
                    dto.setCreatedAt(space.getCreatedAt());
                    dto.setCurrentTurn(space.getCurrentTurn());
                    dto.setStatus(space.getStatus());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSpace(UUID spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));
        List<Document> documents = documentRepository.findBySpaceId(spaceId);
        for (Document document : documents) {
            if (document.getKey() != null && !document.getKey().isBlank()) {
                fileStore.deleteObject(document.getKey());
            }
        }
        edgeRepository.deleteByFromSpaceId(spaceId);
        edgeRepository.deleteByToSpaceId(spaceId);
        nodeRepository.deleteBySpaceId(spaceId);
        documentRepository.deleteBySpaceId(spaceId);
        spaceRepository.delete(space);
    }

    private InsightDto toInsightDto(Node node) {
        ConclusionState state = node.getState() == null ? null : ConclusionState.valueOf(node.getState());
        boolean selectable = state == ConclusionState.AVAILABLE && !node.isLocked();
        return new InsightDto(
                node.getId().toString(),
                node.getSummary(),
                node.getContent(),
                node.getGeneratedTurn(),
                state,
                selectable,
                new ArrayList<>(node.getTags())
        );
    }

    private EvidenceDto toEvidenceDto(Node node) {
        EvidenceState state = node.getState() == null ? null : EvidenceState.valueOf(node.getState());
        boolean draggable = state == EvidenceState.AVAILABLE && !node.isLocked();
        return new EvidenceDto(
                node.getId().toString(),
                node.getSummary(),
                node.getContent(),
                node.getGeneratedTurn(),
                state,
                node.getUsedInTurn(),
                draggable,
                new ArrayList<>(node.getTags())
        );
    }

    private InsightSnapshotDto toInsightSnapshot(Node node) {
        return new InsightSnapshotDto(node.getId().toString(), node.getSummary(), node.getContent(), node.getGeneratedTurn());
    }

    private EvidenceSnapshotDto toEvidenceSnapshot(Node node) {
        return new EvidenceSnapshotDto(node.getId().toString(), node.getSummary(), node.getContent(), node.getGeneratedTurn());
    }
}
