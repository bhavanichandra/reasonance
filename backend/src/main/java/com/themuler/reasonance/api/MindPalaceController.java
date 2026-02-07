package com.themuler.reasonance.api;


import com.themuler.reasonance.dto.*;
import com.themuler.reasonance.dto.*;
import com.themuler.reasonance.service.MindPalaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MindPalaceController {

    private final MindPalaceService mindPalaceService;

    @GetMapping("/ping")
    public ResponseEntity<ServerResult<PingResponse>> ping() {
        log.info("Ping request received");
        return ResponseEntity.ok(new ServerResult<>(true, "Working fine", new PingResponse("UP", Instant.now())));
    }


    @PostMapping(value = "/space", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServerResult<CreateSpaceResponse>> createThinkingSpace(@RequestParam("files") MultipartFile[] files) throws IOException {
        log.info("Received request to create space with {} files", files.length);
        CreateSpaceResponse response = mindPalaceService.createSpaceWithFiles(files);
        log.info("Space created successfully: {}", response.getSpace().getId());
        return ResponseEntity.ok(new ServerResult<>(true, "Space created successfully", response));
    }

    @GetMapping("/spaces")
    public ResponseEntity<ServerResult<List<SpaceDto>>> getAllSpaces() {
        log.info("Received request to get all spaces");
        List<SpaceDto> spaces = mindPalaceService.getAllSpaces();
        return ResponseEntity.ok(new ServerResult<>(true, "Spaces retrieved successfully", spaces));
    }

    @DeleteMapping("/spaces/{spaceId}")
    public ResponseEntity<ServerResult<Void>> deleteSpace(@PathVariable UUID spaceId) {
        log.info("Received request to delete space: {}", spaceId);
        mindPalaceService.deleteSpace(spaceId);
        log.info("Space deleted successfully: {}", spaceId);
        return ResponseEntity.ok(new ServerResult<>(true, "Space deleted successfully", null));
    }

    @PostMapping("/initialize")
    public ResponseEntity<ServerResult<InitializeResponse>> initializeAnalysis(@Valid @RequestBody InitRequest initRequest) {
        log.info("Received request to initialize analysis for space: {}", initRequest.spaceId());
        InitializeResponse response = mindPalaceService.initializeAnalysis(UUID.fromString(initRequest.spaceId()));
        log.info("Analysis initialized successfully for space: {}", initRequest.spaceId());
        return ResponseEntity.ok(new ServerResult<>(true, "Done", response));
    }

    @PostMapping("/analyze")
    public ResponseEntity<ServerResult<AnalyzeResponse>> analyse(@Valid @RequestBody AnalyzeRequest analyzeRequest) {
        log.info("Received request to analyze space: {}", analyzeRequest.spaceId());
        AnalyzeResponse response = mindPalaceService.analyze(analyzeRequest);
        log.info("Analysis completed successfully for space: {}", analyzeRequest.spaceId());
        return ResponseEntity.ok(new ServerResult<>(true, "Done", response));
    }

    @PostMapping("/space/{spaceId}/finalize")
    public ResponseEntity<ServerResult<FinalizeResponse>> finalizeSpace(@PathVariable UUID spaceId) {
        log.info("Received request to finalize space: {}", spaceId);
        FinalizeResponse finalConclusion = mindPalaceService.finalizeSpace(spaceId);
        log.info("Space finalized successfully: {}", spaceId);
        return ResponseEntity.ok(new ServerResult<>(true, "Space finalized successfully", finalConclusion));
    }

    @GetMapping("/space/{spaceId}/board-state")
    public ResponseEntity<ServerResult<BoardStateResponse>> getBoardState(@PathVariable UUID spaceId) {
        log.info("Received request to get board state for space: {}", spaceId);
        BoardStateResponse response = mindPalaceService.getBoardState(spaceId);
        return ResponseEntity.ok(new ServerResult<>(true, "Board state retrieved successfully", response));
    }

    @GetMapping("/space/{spaceId}/tree")
    public ResponseEntity<ServerResult<TreeResponse>> getSpaceTree(@PathVariable UUID spaceId) {
        log.info("Received request to get node tree for space: {}", spaceId);
        TreeResponse tree = mindPalaceService.getNodeTree(spaceId);
        return ResponseEntity.ok(new ServerResult<>(true, "Tree retrieved successfully", tree));
    }
}
