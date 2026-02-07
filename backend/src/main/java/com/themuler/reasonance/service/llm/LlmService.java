package com.themuler.reasonance.service.llm;

import com.themuler.reasonance.core.ServicePrompt;

public interface LlmService {
    String generate(ServicePrompt prompt, Object... args);
}
