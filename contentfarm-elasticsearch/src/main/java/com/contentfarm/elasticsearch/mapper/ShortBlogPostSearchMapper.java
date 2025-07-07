package com.contentfarm.elasticsearch.mapper;

import com.contentfarm.ContentFarmLocaleDateTimeUtils;
import com.contentfarm.aggregateroot.blogpost.ShortBlogPostDomainModel;
import com.contentfarm.elasticsearch.document.ShortBlogPostDocument;
import com.contentfarm.response.SearchResult;
import com.contentfarm.response.ShortBlogPostSearchResultDto;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ShortBlogPostSearchMapper {
    public SearchResult<ShortBlogPostSearchResultDto> toSearchResult(SearchHits<ShortBlogPostDocument> shortBlogPostSearchHits) {
        var searchResult = new SearchResult<ShortBlogPostSearchResultDto>();

        var blogPostSearchResultDtoList = toShortBlogPostSearchResultDto(shortBlogPostSearchHits);
        searchResult.setSearchResultItemList(blogPostSearchResultDtoList);

        searchResult.setSearchResultCount((int) shortBlogPostSearchHits.getTotalHits());
        searchResult.setSearchExecutionTimeInMs(shortBlogPostSearchHits.getExecutionDuration().toMillis());
        searchResult.setMaxSearchScore(shortBlogPostSearchHits.getMaxScore());

        return searchResult;
    }

    public ShortBlogPostSearchResultDto toShortBlogPostSearchResultDto(ShortBlogPostDocument shortBlogPostDocument) {
        var shortBlogPostSearchResultDto = new ShortBlogPostSearchResultDto();
        if (null == shortBlogPostDocument) {
            return null;
        }
        shortBlogPostSearchResultDto.setId(shortBlogPostDocument.getId());
        shortBlogPostSearchResultDto.setTitle(shortBlogPostDocument.getTitle());
        shortBlogPostSearchResultDto.setTagList(null == shortBlogPostDocument.getTagList() ? List.of() : shortBlogPostDocument.getTagList());
        shortBlogPostSearchResultDto.setSummary(shortBlogPostDocument.getSummary());
        shortBlogPostSearchResultDto.setAuthorName(shortBlogPostDocument.getAuthorName());
        shortBlogPostSearchResultDto.setPostDate(shortBlogPostDocument.getPostDate());
        shortBlogPostSearchResultDto.setImageUrl(shortBlogPostDocument.getImageUrl());
        return shortBlogPostSearchResultDto;
    }

    public ShortBlogPostDocument toShortBlogPostDocument(ShortBlogPostDomainModel shortBlogPostDomainModel) {
        return ShortBlogPostDocument
                .builder()
                .id(shortBlogPostDomainModel.getId())
                .title(shortBlogPostDomainModel.getTitle())
                .tagList(shortBlogPostDomainModel.getTagList())
                .summary(shortBlogPostDomainModel.getSummary())
                .authorName(shortBlogPostDomainModel.getAuthorId())
                .postDate(ContentFarmLocaleDateTimeUtils.formatTo_yyyy_MM_dd(shortBlogPostDomainModel.getCreatedDateTime()))
                .imageUrl(shortBlogPostDomainModel.getCoverImageUrl())
                .build();
    }

    public ShortBlogPostDomainModel toShortBlogPostDomainModel(ShortBlogPostDocument shortBlogPostDocument) {
        return ShortBlogPostDomainModel
                .builder()
                .id(shortBlogPostDocument.getId())
                .title(shortBlogPostDocument.getTitle())
                .tagList(shortBlogPostDocument.getTagList())
                .summary(shortBlogPostDocument.getSummary())
                .authorId(shortBlogPostDocument.getAuthorName())
                .createdDateTime(ContentFarmLocaleDateTimeUtils.parseFrom_yyyy_MM_dd(shortBlogPostDocument.getPostDate()))
                .coverImageUrl(shortBlogPostDocument.getImageUrl())
                .build();
    }

    private List<ShortBlogPostSearchResultDto> toShortBlogPostSearchResultDto(SearchHits<ShortBlogPostDocument> searchHits) {
        List<ShortBlogPostSearchResultDto> shortBlogPostSearchResultDtoList = new ArrayList<>();
        if (searchHits.getTotalHits() > 0) {
            shortBlogPostSearchResultDtoList = searchHits.getSearchHits().stream().map(x -> toShortBlogPostSearchResultDto(x.getContent())).toList();
        }
        return shortBlogPostSearchResultDtoList;
    }
}
