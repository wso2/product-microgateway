/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package sourcewatcher

import (
	"io/ioutil"
	"path/filepath"
	"strings"
	"time"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/api"
	xds "github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/pkg/auth"

	"github.com/go-git/go-git/v5"
	"github.com/go-git/go-git/v5/plumbing"
)

const (
	apisArtifactDir string = "apis"
	zipExt          string = ".zip"
)

var artifactsMap map[string]model.ProjectAPI

// Start fetches the API artifacts at the startup and polls for changes from the remote repository
func Start() {
	conf, err := config.ReadConfigs()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error reading configs: %v", err)
	}

	retryInterval := conf.Adapter.SourceControl.RetryInterval

	loggers.LoggerAPI.Info("Starting source watcher")
	// Fetch the API artifacts at the startup
	repository, err := fetchArtifacts()

	// Retry fetching the API artifacts if the first attempt fails
	for {
		if err == nil {
			break
		} else {
			if err != nil{
				loggers.LoggerAPI.Errorf("Error while fetching API artifacts during startup: %v", err)
			}
			time.Sleep(time.Duration(retryInterval) * time.Second)
			loggers.LoggerAPI.Info("Retrying fetching artifacts from the remote repository")
			repository, err = fetchArtifacts()
		}
	}

	artifactsMap, err = api.ProcessMountedAPIProjects()
	if err != nil {
		logger.LoggerMgw.Error("Readiness probe is not set as local api artifacts processing has failed.")
	}

	loggers.LoggerAPI.Info("Polling for changes")
	go pollChanges(repository)
}

// fetchArtifacts clones the API artifacts from the remote repository into the artifacts directory in the adapter
func fetchArtifacts() (repository *git.Repository,err error) {
	conf, err := config.ReadConfigs()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error reading configs: %v", err)
		return nil, err
	}

	artifactsDirName := filepath.FromSlash(conf.Adapter.SourceControl.ArtifactsDirectory + "/" + apisArtifactDir)
	repositoryURL := conf.Adapter.SourceControl.Repository.URL
	branch := conf.Adapter.SourceControl.Repository.Branch

	// Opens the local repository, if exists
	repository, _ = git.PlainOpen(artifactsDirName)

	// If a local repository exists, pull the changes from the remote repository
	if repository != nil {
		loggers.LoggerAPI.Info("Starting to fetch changes from remote repository")

		compareRepository(repository)

		return repository, nil
	}

	// If a local repository does not exist, clone the remote repository
	gitAuth, err := auth.GetGitAuth()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error while authenticating the remote repository: %v", err)
		return nil, err
	}

	cloneOptions := &git.CloneOptions{
		URL: repositoryURL,
		Auth: gitAuth,
	}

	if branch != "" {
		cloneOptions.ReferenceName = plumbing.ReferenceName("refs/heads/" + branch)
	}

	loggers.LoggerAPI.Info("Fetching API artifacts from remote repository")

	// Clones the  remote repository
	repository, err = git.PlainClone(artifactsDirName, false, cloneOptions)

	if err != nil {
		loggers.LoggerAPI.Errorf("Error while fetching artifacts from the remote repository: %v", err)
	}

	return repository, err

}

// pollChanges polls for changes from the remote repository
func pollChanges(repository *git.Repository){
	conf, err := config.ReadConfigs()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error reading configs: %v", err)
		return
	}

	pollInterval := conf.Adapter.SourceControl.PollInterval
	for {
		<- time.After(time.Duration(pollInterval) * time.Second)
		go compareRepository(repository)
	}
}

// compareRepository compares the hashes of the local and remote repositories and pulls if there are any changes
func compareRepository(localRepository *git.Repository){
	remote, err := localRepository.Remote("origin")
	if err != nil{
		loggers.LoggerAPI.Errorf("Error while returning remote: %v", err)
	}

	gitAuth, err := auth.GetGitAuth()

	remoteList, err := remote.List(&git.ListOptions{
		Auth: gitAuth,
	})
	if err != nil {
		loggers.LoggerAPI.Errorf("Error while listing remote: %v", err)
	}

	ref, err := localRepository.Head()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error while retrieving Head: %v", err)
	}

	refName := ref.Name()

	localRepositoryHash := ref.Hash().String()

	for _, r := range remoteList {
		if r.Name() == refName && !r.Hash().IsZero() && localRepositoryHash != r.Hash().String() {
			loggers.LoggerAPI.Info("Fetching commit with hash: ", r.Hash().String(), " from remote repository")
			pullChanges(localRepository)

			err := processArtifactChanges()
			if err != nil {
				loggers.LoggerAPI.Errorf("Error while processing artifact changes: %v", err)
			}

			//Redeploy changes
			artifactsMap, err = api.ProcessMountedAPIProjects()
			if err != nil {
				loggers.LoggerAPI.Errorf("Local api artifacts processing has failed: %v", err)
				return
			}
		}
	}

}

// pullChanges pulls changes from the given repository
func pullChanges(localRepository *git.Repository){
	conf, err := config.ReadConfigs()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error reading configs: %v", err)
		return
	}

	branch := conf.Adapter.SourceControl.Repository.Branch

	workTree, err := localRepository.Worktree()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error while retrieving the worktree: %v", err)
	}

	gitAuth, err := auth.GetGitAuth()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error while authenticating the remote repository: %v", err)
	}

	pullOptions := &git.PullOptions{
		Auth: gitAuth,
	}

	if branch != "" {
		pullOptions.ReferenceName = plumbing.ReferenceName("refs/heads/" + branch)
	}

	err = workTree.Pull(pullOptions)
	if err != nil {
		loggers.LoggerAPI.Errorf("Error while pulling changes from repository: %v", err)
	}
}

// processArtifactChanges undeploy the APIs whose artifacts are not present in the repository
func processArtifactChanges() (err error){
	conf, err := config.ReadConfigs()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error reading configs: %v", err)
		return err
	}

	apisDirName := filepath.FromSlash(conf.Adapter.SourceControl.ArtifactsDirectory + "/" + apisArtifactDir)
	files, err := ioutil.ReadDir(apisDirName)
	if err != nil {
		loggers.LoggerAPI.Errorf("Error while reading api artifacts during startup: %v", err)
	}

	// Assign the API artifacts to a temporary map
	removedArtifacts := artifactsMap

	for _, apiProjectFile := range files {
		if apiProjectFile.IsDir() {
			// If the artifact is present in the artifacts directory, remove it from the temporary map
			if _, ok := removedArtifacts[apiProjectFile.Name()]; ok {
				delete(removedArtifacts, apiProjectFile.Name())
			}
			continue
		} else if !strings.HasSuffix(apiProjectFile.Name(), zipExt) {
			continue
		}
		if _, ok := removedArtifacts[apiProjectFile.Name()]; ok {
			delete(removedArtifacts, apiProjectFile.Name())
		}
	}

	// Undeploy the APIs whose artifacts are not present in the repository
	for _, apiProject := range removedArtifacts {
		apiYaml := apiProject.APIYaml.Data

		vhostToEnvsMap := make(map[string][]string)
		for _, environment := range apiProject.Deployments {
			vhostToEnvsMap[environment.DeploymentVhost] =
				append(vhostToEnvsMap[environment.DeploymentVhost], environment.DeploymentEnvironment)
		}

		for vhost, environments := range vhostToEnvsMap {
			if vhost == "" {
				// ignore if vhost is empty, since it deletes all vhosts of API
				continue
			}
			err = xds.DeleteAPIs(vhost, apiYaml.Name, apiYaml.Version, environments, apiProject.OrganizationID)

			if err != nil {
				loggers.LoggerAPI.Errorf("Error while deleting API: %v", err)
			}
		}
	}
	return nil
}


