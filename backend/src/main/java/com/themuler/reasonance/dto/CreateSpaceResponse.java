package com.themuler.reasonance.dto;

import com.themuler.reasonance.entity.Document;
import com.themuler.reasonance.entity.Space;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSpaceResponse {
    private Space space;
    private List<Document> documents;
}
