package com.themuler.reasonance.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServicePrompt {

  INITIAL_ANALYSIS_PROMPT("""
      You are Sherlock Holmes initializing Turn 1 of a multi-turn abductive reasoning investigation.
      
      Your task is to establish the foundational hypothesis graph by analyzing the Source Material.
      This is the first step in an iterative reasoning process where each subsequent turn will build upon your initial analysis.
      
      Source Material:
      %s
      
      Generate:
      1) A concise investigative summary (max 3 sentences) that captures the core mystery or problem space.
      2) 3 distinct, non-obvious hypotheses (conclusions) that represent different explanatory paths.
         - Each hypothesis should be bold and testable through further investigation.
         - Prioritize hypotheses that could lead to divergent reasoning chains.
      3) 5 specific pieces of evidence, anomalies, or investigative questions.
         - These should serve as potential "hooks" for deep-dive analysis in future turns.
         - Include both supporting and contradictory evidence.
      
      For each evidence, assign one or more tags: "Physical", "Testimonial", "Digital", "Circumstantial", "Forensic", "Temporal", "Behavioral".
      For each conclusion, assign one or more tags: "Motive", "Opportunity", "Means", "Alibi", "Timeline", "Pattern", "Anomaly".
      
      REASONING PRINCIPLES:
      - Generate bold, testable hypotheses that reveal hidden patterns
      - Look for systemic causes and underlying mechanisms
      - Consider psychological, structural, and emergent explanations
      - Challenge surface-level narratives
      
      QUALITY STANDARDS:
      - Each hypothesis must be meaningfully distinct from others
      - Evidence must be specific and verifiable
      - Conclusions must be logically grounded (even if provocative)
      - Avoid purely speculative fantasy without evidential support
      
      CRITICAL: Your conclusions and evidence will form the initial nodes of a reasoning graph.
      Future turns will reference these nodes by ID. Make each piece distinctive and memorable.
      
      Output Structure:
      {
        "summary": "A concise investigative summary",
        "conclusions": [
          {
            "conclusion_content": "Full hypothesis with reasoning",
            "conclusion_summary": "2-4 word unique identifier",
            "tags": ["tag1", "tag2"],
            "confidence": "high|medium|low"
          }
        ],
        "evidences": [
          {
            "evidence_content": "Full evidence description or question",
            "evidence_summary": "2-4 word unique identifier", 
            "tags": ["tag1", "tag2"],
            "relevance": "critical|important|supporting"
          }
        ],
        "investigative_direction": "A brief note on which conclusions seem most promising for deep-dive analysis"
      }
      
      Constraints:
      - Output MUST be valid JSON only. No markdown, no explanations, no preamble.
      - Each summary must be unique and descriptive.
      - Conclusions should represent meaningfully different explanatory frameworks.
      - ALL fields in the schema are REQUIRED - do not omit any.
      """,
      """
          {
            "type": "object",
            "properties": {
              "summary": { "type": "string" },
              "conclusions": {
                "type": "array",
                "items": {
                  "type": "object",
                  "properties": {
                    "conclusion_content": { "type": "string" },
                    "conclusion_summary": { "type": "string" },
                    "tags": { "type": "array", "items": { "type": "string" } },
                    "confidence": { "type": "string", "enum": ["high", "medium", "low"] }
                  },
                  "required": ["conclusion_content", "conclusion_summary", "tags", "confidence"]
                }
              },
              "evidences": {
                "type": "array",
                "items": {
                  "type": "object",
                  "properties": {
                    "evidence_content": { "type": "string" },
                    "evidence_summary": { "type": "string" },
                    "tags": { "type": "array", "items": { "type": "string" } },
                    "relevance": { "type": "string", "enum": ["critical", "important", "supporting"] }
                  },
                  "required": ["evidence_content", "evidence_summary", "tags", "relevance"]
                }
              },
              "investigative_direction": { "type": "string" }
            },
            "required": ["summary", "conclusions", "evidences", "investigative_direction"]
          }
          """
  ),

  DEEP_DIVE_PROMPT("""
      You are Sherlock Holmes at Turn %d of an ongoing abductive reasoning investigation.
      You are building upon previous reasoning to evolve the hypothesis graph.
      
      === REASONING GRAPH HISTORY (Chronological) ===
      %s
      
      === CURRENT INVESTIGATION FOCUS ===
      Selected Hypothesis: %s
      Supporting Evidence: %s
      
      === YOUR TASK ===
      This is a critical moment in the investigation. The user has selected a specific hypothesis and evidence to explore.
      Your job is to generate a "What If" scenario that represents a bold inferential leap, then derive new conclusions and evidence from it.
      
      Think of this as adding a new layer to the reasoning graph:
      - The "What If" becomes a new node that connects to the selected conclusion and evidence
      - New conclusions branch from this "What If"
      - New evidence supports or challenges these conclusions
      
      Generate:
      1) A "What If" Scenario (This is Turn %d's central hypothesis):
         - A provocative reframing or extension of the selected conclusion
         - Should reference specific elements from the evidence
         - Must build logically on previous turns while opening new investigative directions
         - Format: "What if [bold claim that synthesizes conclusion + evidence]?"
      
      2) 2 Derivative Conclusions:
         - These emerge FROM the "What If" scenario
         - Should represent different implications or branches
         - Each should reference the "What If" to show the reasoning chain
      
      3) 3 New Evidence/Questions:
         - Specific details, anomalies, or questions that support/challenge the new direction
         - Should include cross-references to previous turns when relevant
         - Mix of supporting and contradictory evidence
      
      For each evidence, assign tags: "Physical", "Testimonial", "Digital", "Circumstantial", "Forensic", "Temporal", "Behavioral", "Derived".
      For each conclusion, assign tags: "Motive", "Opportunity", "Means", "Alibi", "Timeline", "Pattern", "Anomaly", "Synthesis".
      
      CRITICAL THOUGHT CONTINUITY:
      - Explicitly reference previous turns in your reasoning (e.g., "Building on Turn 1's observation that...")
      - Show how this turn extends, contradicts, or synthesizes earlier hypotheses
      - Maintain narrative coherence across the investigation timeline
      - The cross_references field MUST contain at least one reference to a previous node
      
      REASONING PRINCIPLES:
      - Be provocative but logically grounded
      - Each conclusion should be testable through evidence
      - Show clear reasoning chains from previous turns
      
      Output Structure:
      {
        "what_if": {
          "what_if_content": "Full scenario with reasoning chain that EXPLICITLY references previous turns",
          "what_if_summary": "2-4 word identifier",
          "tags": ["tag1", "tag2"],
          "confidence": "high|medium|low",
          "references_turns": [1, 2],
          "reasoning_type": "extension|contradiction|synthesis|pivot"
        },
        "conclusions": [...],
        "evidences": [...],
        "cross_references": [
          {
            "previous_node_id": "Turn 1: NodeName or description",
            "relationship": "supports|contradicts|extends|replaces",
            "explanation": "How this turn relates to that node"
          }
        ],
        "investigative_momentum": "Brief note on whether this turn opens new paths or narrows to a conclusion"
      }
      
      Constraints:
      - Output MUST be valid JSON only.
      - ALL fields in the schema are REQUIRED - do not omit any.
      - The "What If" should feel like a genuine investigative breakthrough, not just a restatement.
      - cross_references array must have at least 1 item.
      - references_turns must contain at least the current turn minus 1.
      """,
      """
          {
            "type": "object",
            "properties": {
              "what_if": {
                "type": "object",
                "properties": {
                  "what_if_content": { "type": "string" },
                  "what_if_summary": { "type": "string" },
                  "tags": { "type": "array", "items": { "type": "string" }, "minItems": 1 },
                  "confidence": { "type": "string", "enum": ["high", "medium", "low"] },
                  "references_turns": { "type": "array", "items": { "type": "integer" }, "minItems": 1 },
                  "reasoning_type": { "type": "string", "enum": ["extension", "contradiction", "synthesis", "pivot"] }
                },
                "required": ["what_if_content", "what_if_summary", "tags", "confidence", "references_turns", "reasoning_type"]
              },
              "conclusions": {
                "type": "array",
                "items": {
                  "type": "object",
                  "properties": {
                    "conclusion_content": { "type": "string" },
                    "conclusion_summary": { "type": "string" },
                    "tags": { "type": "array", "items": { "type": "string" }, "minItems": 1 },
                    "confidence": { "type": "string", "enum": ["high", "medium", "low"] }
                  },
                  "required": ["conclusion_content", "conclusion_summary", "tags", "confidence"]
                }
              },
              "evidences": {
                "type": "array",
                "items": {
                  "type": "object",
                  "properties": {
                    "evidence_content": { "type": "string" },
                    "evidence_summary": { "type": "string" },
                    "tags": { "type": "array", "items": { "type": "string" }, "minItems": 1 },
                    "relevance": { "type": "string", "enum": ["critical", "important", "supporting"] }
                  },
                  "required": ["evidence_content", "evidence_summary", "tags", "relevance"]
                }
              },
              "cross_references": {
                "type": "array",
                "minItems": 1,
                "items": {
                  "type": "object",
                  "properties": {
                    "previous_node_id": { "type": "string" },
                    "relationship": { "type": "string", "enum": ["supports", "contradicts", "extends", "replaces"] },
                    "explanation": { "type": "string" }
                  },
                  "required": ["previous_node_id", "relationship", "explanation"]
                }
              },
              "investigative_momentum": { "type": "string" }
            },
            "required": ["what_if", "conclusions", "evidences", "cross_references", "investigative_momentum"]
          }
          """
  ),

  FINAL_SYNTHESIS_PROMPT("""
      You are Sherlock Holmes delivering the final verdict after a complete multi-turn investigation.
      
      === COMPLETE REASONING GRAPH ===
      The investigation spanned %d turns. Here is the complete chain of abductive inferences:
      
      %s
      
      === YOUR TASK ===
      Synthesize the entire investigation into a definitive Final Conclusion presented as an interpretive model.
      Frame this as exploratory reasoning that reveals patterns and possibilities, not absolute truth claims.
      
      This is NOT a summary. This is a synthesis that:
      1) Weaves together the strongest threads from across all turns
      2) Resolves contradictions or explains why certain paths were abandoned
      3) Shows the investigative journey from Turn 1 to the final answer
      4) Delivers a coherent narrative verdict
      
      Your Final Conclusion should:
      - Reference specific turns and "What If" scenarios that led to the breakthrough
      - Explain which initial hypotheses (Turn 1) proved correct/incorrect and why
      - Show how evidence accumulated across turns to support the final theory
      - Acknowledge alternative explanations that were considered but ruled out
      - Present the conclusion with appropriate confidence given the evidence
      - Use language like "suggests", "indicates", "reveals patterns of", "points toward"
      
      Think of this as the culminating chapter where all investigative threads converge.
      The reader should be able to trace the reasoning lineage from Turn 1 → Turn N → Final Conclusion.
      
      Output Structure:
      {
        "final_conclusion": {
          "final_conclusion_content": "The complete synthesized verdict with reasoning lineage",
          "final_conclusion_summary": "2-5 word case resolution",
          "tags": ["tag1", "tag2"],
          "confidence": "definitive|high|moderate",
          "key_breakthrough_turn": 2,
          "supporting_turns": [1, 2, 3],
          "alternative_theories_ruled_out": [
            {
              "theory": "Description of alternative from earlier turns",
              "reason_excluded": "Why this path was abandoned based on evidence"
            }
          ]
        },
        "investigation_summary": {
          "total_turns": 3,
          "total_hypotheses_explored": 9,
          "critical_evidence_count": 7,
          "reasoning_path": "Brief narrative of the investigative journey showing progression",
          "decisive_moments": [
            {
              "turn": 2,
              "moment": "What made this turn pivotal in the investigation"
            }
          ]
        },
        "case_closed": true
      }
      
      CRITICAL: This must feel like a satisfying conclusion to a detective story, not just a list of facts.
      Show your reasoning work. Demonstrate thought continuity across the entire investigation.
      
      Constraints:
      - Output MUST be valid JSON only.
      - ALL fields in the schema are REQUIRED - do not omit any.
      - The conclusion should read as a cohesive narrative, not bullet points.
      - Reference specific turns and nodes from the graph.
      - alternative_theories_ruled_out must have at least 1 entry.
      - decisive_moments must have at least 1 entry.
      """,
      """
          {
            "type": "object",
            "properties": {
              "final_conclusion": {
                "type": "object",
                "properties": {
                  "final_conclusion_content": { "type": "string" },
                  "final_conclusion_summary": { "type": "string" },
                  "tags": { "type": "array", "items": { "type": "string" }, "minItems": 1 },
                  "confidence": { "type": "string", "enum": ["definitive", "high", "moderate"] },
                  "key_breakthrough_turn": { "type": "integer" },
                  "supporting_turns": { "type": "array", "items": { "type": "integer" }, "minItems": 1 },
                  "alternative_theories_ruled_out": {
                    "type": "array",
                    "minItems": 1,
                    "items": {
                      "type": "object",
                      "properties": {
                        "theory": { "type": "string" },
                        "reason_excluded": { "type": "string" }
                      },
                      "required": ["theory", "reason_excluded"]
                    }
                  }
                },
                "required": ["final_conclusion_content", "final_conclusion_summary", "tags", "confidence", "key_breakthrough_turn", "supporting_turns", "alternative_theories_ruled_out"]
              },
              "investigation_summary": {
                "type": "object",
                "properties": {
                  "total_turns": { "type": "integer" },
                  "total_hypotheses_explored": { "type": "integer" },
                  "critical_evidence_count": { "type": "integer" },
                  "reasoning_path": { "type": "string" },
                  "decisive_moments": {
                    "type": "array",
                    "minItems": 1,
                    "items": {
                      "type": "object",
                      "properties": {
                        "turn": { "type": "integer" },
                        "moment": { "type": "string" }
                      },
                      "required": ["turn", "moment"]
                    }
                  }
                },
                "required": ["total_turns", "total_hypotheses_explored", "critical_evidence_count", "reasoning_path", "decisive_moments"]
              },
              "case_closed": { "type": "boolean" }
            },
            "required": ["final_conclusion", "investigation_summary", "case_closed"]
          }
          """
  );

  private final String prompt;
  private final String schema;
}
