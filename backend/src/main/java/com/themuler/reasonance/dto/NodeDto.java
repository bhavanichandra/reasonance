package com.themuler.reasonance.dto;

import com.themuler.reasonance.entity.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDto {
    private UUID id;
    private NodeType type;
    private String content;
    private String summary;
    private int turn;
    private boolean locked;
    private Set<String> tags;
    private List<NodeDto> children;
    private UUID parentWhatIfId;
}
