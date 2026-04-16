package com.ai.kb.dto;

import java.util.List;

public record RetrievalDebugResponse(
        String originalQuery,
        String rewrittenQuery,
        String usedQuery,
        List<RetrievalHitResponse> originalHits,
        List<RetrievalHitResponse> rewrittenHits,
        List<RetrievalHitResponse> finalHits
) {
}
