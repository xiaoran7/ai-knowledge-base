package com.ai.kb.dto;

import java.util.List;

public record DocumentTaskListResponse(
        List<DocumentTaskResponse> list
) {
}
