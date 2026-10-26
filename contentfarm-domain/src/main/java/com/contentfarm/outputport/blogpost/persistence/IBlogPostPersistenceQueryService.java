package com.contentfarm.outputport.blogpost.persistence;

import com.contentfarm.aggregateroot.blogpost.BlogPostDomainModel;

import java.util.List;
import java.util.Optional;

public interface IBlogPostPersistenceQueryService {
    Optional<BlogPostDomainModel> getById(String id);
    List<String> findTagList();
}
