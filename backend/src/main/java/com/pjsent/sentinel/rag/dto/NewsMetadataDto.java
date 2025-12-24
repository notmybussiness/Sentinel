package com.pjsent.sentinel.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 뉴스 검색 메타데이터 DTO
 * Python RAG API Response의 metadata 객체
 *
 * @see <a href="file://.claude/specs/API_NEWS_RAG.md">API_NEWS_RAG.md</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsMetadataDto {

    /**
     * Milvus 검색 결과 총 개수
     */
    @JsonProperty("totalMatches")
    private Integer totalMatches;

    /**
     * 실제 반환된 뉴스 개수
     */
    @JsonProperty("returnedCount")
    private Integer returnedCount;

    /**
     * 검색 소요 시간 (밀리초)
     */
    @JsonProperty("searchTimeMs")
    private Integer searchTimeMs;
}
