/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	"fmt"
	"io/ioutil"
	"path/filepath"
	"strings"
	"time"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/api"
	xds "github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/pkg/auth"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"

	"github.com/go-git/go-git/v5"
	"github.com/go-git/go-git/v5/plumbing"
)

const (
	apisArtifactDir string = "apis"
	zipExt          string = ".zip"
	branchHead      string = "refs/heads/"
)

var artifactsMap map[string]model.ProjectAPI

// Start fetches the API artifacts at the startup and polls for changes from the remote repository
func Start() error{
	conf, _ := config.ReadConfigs()

	retryInterval := conf.Adapter.SourceControl.RetryInterval
	maxRetryCount := conf.Adapter.SourceControl.MaxRetryCount

	loggers.LoggerSourceWatcher.Info("Starting source watcher")
	// Fetch the API artifacts at the startup
	repository, err := fetchArtifacts()

	// Retry fetching the API artifacts if the first attempt fails
	for retries := 0; retries < maxRetryCount; retries++ {
		if err == nil {
			break
		} else {
			time.Sleep(time.Duration(retryInterval) * time.Second)
			loggers.LoggerSourceWatcher.Info("Retrying fetching artifacts from the remote repository")
			repository, err = fetchArtifacts()
		}
	}

	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while fetching artifacts from the remote repository at startup : %s", err.Error()),
			Severity: logging.CRITICAL,
			ErrorCode: 2511,
		})
		return err
	}

	artifactsMap, err = api.ProcessMountedAPIProjects()
	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Processing API artifacts failed : %s", err.Error()),
			Severity: logging.CRITICAL,
			ErrorCode: 2500,
		})
		return err
	}

	loggers.LoggerSourceWatcher.Info("Polling for changes")
	go pollChanges(repository)
	return nil
}

// fetchArtifacts clones the API artifacts from the remote repository into the artifacts directory in the adapter
func fetchArtifacts() (repository *git.Repository,err error) {
	conf, _ := config.ReadConfigs()

	artifactsDirName := filepath.FromSlash(conf.Adapter.SourceControl.ArtifactsDirectory + "/" + apisArtifactDir)
	repositoryURL := conf.Adapter.SourceControl.Repository.URL
	branch := conf.Adapter.SourceControl.Repository.Branch

	// Opens the local repository, if exists
	repository, _ = git.PlainOpen(artifactsDirName)

	// If a local repository exists, pull the changes from the remote repository
	if repository != nil {
		loggers.LoggerSourceWatcher.Info("Local repository exists, pulling changes from the remote repository %s.", repositoryURL)

		pullRepositoryIfUpdated(repository)

		return repository, nil
	}

	// If a local repository does not exist, clone the remote repository
	gitAuth, err := auth.GetGitAuth()
	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while authenticating with the remote repository : %s", err.Error()),
			Severity: logging.CRITICAL,
			ErrorCode: 2501,
		})
		return nil, err
	}

	cloneOptions := &git.CloneOptions{
		URL: repositoryURL,
		Auth: gitAuth,
	}

	if branch != "" {
		cloneOptions.ReferenceName = plumbing.ReferenceName(branchHead + branch)
	}

	loggers.LoggerSourceWatcher.Infof("Cloning remote repository with API artifacts from  %s", repositoryURL)

	// Clones the  remote repository
	repository, err = git.PlainClone(artifactsDirName, false, cloneOptions)

	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while cloning the remote repository with API artifacts from %s : %s", repositoryURL, err.Error()),
			Severity: logging.CRITICAL,
			ErrorCode: 2502,
		})
	}

	return repository, err

}

// pollChanges polls for changes from the remote repository
func pollChanges(repository *git.Repository){
	conf, _ := config.ReadConfigs()

	pollInterval := conf.Adapter.SourceControl.PollInterval
	for {
		loggers.LoggerSourceWatcher.Debugf("Polling changes from the remote repository %s", conf.Adapter.SourceControl.Repository.URL)
		<- time.After(time.Duration(pollInterval) * time.Second)
		go pullRepositoryIfUpdated(repository)
	}
}

// pullRepositoryIfUpdated compares the hashes of the local and remote repositories and pulls if there are any changes
func pullRepositoryIfUpdated(localRepository *git.Repository){
	remote, err := localRepository.Remote("origin")
	if err != nil{
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while returning remote : %s", err.Error()),
			Severity: logging.CRITICAL,
			ErrorCode: 2503,
		})
	}

	gitAuth, err := auth.GetGitAuth()
	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while authenticating with the remote repository : %s", err.Error()),
			Severity: logging.CRITICAL,
			ErrorCode: 2501,
		})
	}

	remoteList, err := remote.List(&git.ListOptions{
		Auth: gitAuth,
	})
	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while listing remote : %s", err.Error()),
			Severity: logging.MAJOR,
			ErrorCode: 2504,
		})
	}

	ref, err := localRepository.Head()
	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while retrieving local repository head : %s", err.Error()),
			Severity: logging.MAJOR,
			ErrorCode: 2505,
		})
	}

	refName := ref.Name()

	localRepositoryHash := ref.Hash().String()

	for _, r := range remoteList {
		if r.Name() == refName && !r.Hash().IsZero() && localRepositoryHash != r.Hash().String() {
			loggers.LoggerSourceWatcher.Info("Fetching commit with hash: ", r.Hash().String(), " from remote repository")
			if err := pullChanges(localRepository); err != nil {
				return
			}

			err := processArtifactChanges()
			if err != nil {
				loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
					Message: fmt.Sprintf("Error while processing artifact changes : %s", err.Error()),
					Severity: logging.MAJOR,
					ErrorCode: 2506,
				})
				return
			}

			//Redeploy changes
			artifactsMap, err = api.ProcessMountedAPIProjects()
			if err != nil {
				loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
					Message: fmt.Sprintf("Processing API artifacts failed : %s", err.Error()),
					Severity: logging.CRITICAL,
					ErrorCode: 2500,
				})
				return
			}
		}
	}

}

// pullChanges pulls changes from the given repository
func pullChanges(localRepository *git.Repository) error {
	conf, _ := config.ReadConfigs()

	branch := conf.Adapter.SourceControl.Repository.Branch

	gitAuth, err := auth.GetGitAuth()
	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while authenticating with the remote repository : %s", err.Error()),
			Severity: logging.CRITICAL,
			ErrorCode: 2501,
		})
	}

	pullOptions := &git.PullOptions{
		Auth: gitAuth,
	}

	if branch != "" {
		pullOptions.ReferenceName = plumbing.ReferenceName(branchHead + branch)
	}

	workTree, err := localRepository.Worktree()
	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while retrieving local repository worktree : %s", err.Error()),
			Severity: logging.MAJOR,
			ErrorCode: 2507,
		})
	}

	err = workTree.Pull(pullOptions)
	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while pulling changes from the remote repository : %s", err.Error()),
			Severity: logging.MAJOR,
			ErrorCode: 2508,
		})
	}
	return err
}

// processArtifactChanges undeploy the APIs whose artifacts are not present in the repository
func processArtifactChanges() (err error){
	conf, _ := config.ReadConfigs()

	apisDirName := filepath.FromSlash(conf.Adapter.SourceControl.ArtifactsDirectory + "/" + apisArtifactDir)
	files, err := ioutil.ReadDir(apisDirName)
	if err != nil {
		loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
			Message: fmt.Sprintf("Error while reading API artifacts directory : %s", err.Error()),
			Severity: logging.MAJOR,
			ErrorCode: 2509,
		})
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
		// If the zip artifact is present in the artifacts directory, remove it from the temporary map
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
			err = xds.DeleteAPIs(vhost, apiYaml.Name, apiYaml.Version, environments, apiProject.APIYaml.Data.OrganizationID)

			if err != nil {
				loggers.LoggerSourceWatcher.ErrorC(logging.ErrorDetails{
					Message: fmt.Sprintf("Error while deleting API : %s", err.Error()),
					Severity: logging.MAJOR,
					ErrorCode: 2510,
				})
			}
		}
	}
	return nil
}


