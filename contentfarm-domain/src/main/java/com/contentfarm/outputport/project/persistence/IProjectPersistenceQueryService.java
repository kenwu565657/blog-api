package com.contentfarm.outputport.project.persistence;

import com.contentfarm.aggregateroot.project.ProjectDomainModel;

import java.util.List;
import java.util.Optional;

public interface IProjectPersistenceQueryService {
    Optional<ProjectDomainModel> getById(String id);
    List<ProjectDomainModel> findByAuthorId(String authorId);
}
