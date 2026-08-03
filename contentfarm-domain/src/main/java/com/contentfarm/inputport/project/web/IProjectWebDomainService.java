package com.contentfarm.inputport.project.web;

import com.contentfarm.aggregateroot.project.ProjectDomainModel;

import java.util.List;

public interface IProjectWebDomainService {
    ProjectDomainModel getProjectById(String id);
    List<ProjectDomainModel> findProjectByAuthorId(String authorId);
}
