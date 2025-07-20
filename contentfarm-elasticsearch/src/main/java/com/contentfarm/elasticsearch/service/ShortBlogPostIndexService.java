package com.contentfarm.elasticsearch.service;

import com.contentfarm.ContentFarmStringUtils;
import com.contentfarm.aggregateroot.blogpost.ShortBlogPostDomainModel;
import com.contentfarm.elasticsearch.document.ShortBlogPostDocument;
import com.contentfarm.elasticsearch.exception.DocumentIndexException;
import com.contentfarm.elasticsearch.mapper.ShortBlogPostSearchMapper;
import com.contentfarm.elasticsearch.repository.IShortBlogPostElasticsearchRepository;
import com.contentfarm.outputport.blogpost.search.IShortBlogPostIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShortBlogPostIndexService implements IShortBlogPostIndexService {
    private final ShortBlogPostSearchMapper shortBlogPostSearchMapper;
    private final IShortBlogPostElasticsearchRepository shortBlogPostElasticsearchRepository;

    @Override
    public void indexShortBlogPost(ShortBlogPostDomainModel shortBlogPostDomainModel) {
        if (null == shortBlogPostDomainModel) {
            throw DocumentIndexException.ofNullDocument();
        }
        var shortBlogPostDocument = shortBlogPostSearchMapper.toShortBlogPostDocument(shortBlogPostDomainModel);
        List<String> missingFieldList = findMissingFieldList(shortBlogPostDocument);
        if (!missingFieldList.isEmpty()) {
            throw DocumentIndexException.ofMissingField(missingFieldList);
        }
        if (null == shortBlogPostDocument.getTagList()) {
            shortBlogPostDocument.setTagList(List.of());
        }
        try {
            shortBlogPostElasticsearchRepository.save(shortBlogPostDocument);
        } catch (Exception e) {
            throw DocumentIndexException.ofUnExpectedException(e);
        }
    }

    @Override
    public void deleteShortBlogPostIndex(String id) {
        shortBlogPostElasticsearchRepository.deleteById(id);
    }

    private List<String> findMissingFieldList(ShortBlogPostDocument shortBlogPostDocument) {
        Map<String, String> fieldNamingMap = Map.of(
                ShortBlogPostDocument.Fields.id, "Id",
                ShortBlogPostDocument.Fields.title, "Title",
                ShortBlogPostDocument.Fields.summary, "Summary",
                ShortBlogPostDocument.Fields.authorName, "Author Name",
                ShortBlogPostDocument.Fields.postDate, "Post Date",
                ShortBlogPostDocument.Fields.imageUrl, "Image Url"
        );
        List<String> missingFields = new ArrayList<>();
        if (ContentFarmStringUtils.isBlank(shortBlogPostDocument.getId())) {
            missingFields.add(fieldNamingMap.get(ShortBlogPostDocument.Fields.id));
        }
        if (ContentFarmStringUtils.isBlank(shortBlogPostDocument.getTitle())) {
            missingFields.add(fieldNamingMap.get(ShortBlogPostDocument.Fields.title));
        }
        if (ContentFarmStringUtils.isBlank(shortBlogPostDocument.getSummary())) {
            missingFields.add(fieldNamingMap.get(ShortBlogPostDocument.Fields.summary));
        }
        if (ContentFarmStringUtils.isBlank(shortBlogPostDocument.getAuthorName())) {
            missingFields.add(fieldNamingMap.get(ShortBlogPostDocument.Fields.authorName));
        }
        if (ContentFarmStringUtils.isBlank(shortBlogPostDocument.getPostDate())) {
            missingFields.add(fieldNamingMap.get(ShortBlogPostDocument.Fields.postDate));
        }
        if (ContentFarmStringUtils.isBlank(shortBlogPostDocument.getImageUrl())) {
            missingFields.add(fieldNamingMap.get(ShortBlogPostDocument.Fields.imageUrl));
        }
        return missingFields;
    }
}
