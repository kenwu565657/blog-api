package com.contentfarm.outputport.project.persistence;

import com.contentfarm.aggregateroot.project.ProjectDomainModel;

public interface IProjectPersistenceCommandService {
    ProjectDomainModel upsertProject(ProjectDomainModel projectDomainModel);
    String deleteProject(String id);
}
