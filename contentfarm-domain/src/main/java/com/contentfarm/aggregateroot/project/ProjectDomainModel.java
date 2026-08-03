package com.contentfarm.aggregateroot.project;

import com.contentfarm.constant.BlogPostContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDomainModel {
    private String id;
    private String title;
    private String summary;
    private String authorId;
    private List<String> tagList;
    private String coverImageUrl;
    private String sourceCodeUrl;
    private List<String> demoVideoUrl;
    private List<String> imageUrl;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;
}
