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

	"github.com/go-git/go-git/v5"
	"github.com/go-git/go-git/v5/plumbing/transport/http"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/api"
	xds "github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
)

const (
	apisArtifactDir string = "apis"
	zipExt          string = ".zip"
)

var artifactsMap map[string]model.ProjectAPI

// Start fetches the API artifacts at the startup and polls for changes from the remote repository
func Start(conf *config.Config) error{
	loggers.LoggerAPI.Info("Starting source watcher")
	repository, err := fetchArtifacts(conf)

	if err != nil{
		loggers.LoggerAPI.Error("Error while fetching API artifacts during startup. ", err)
		return err
	}

	artifactsMap, err = api.ProcessMountedAPIProjects()
	if err != nil {
		logger.LoggerMgw.Error("Readiness probe is not set as local api artifacts processing has failed.")
		return err
	}

	loggers.LoggerAPI.Info("Polling for changes")
	go pollChanges(conf, repository)
	return nil
}

// getAuth returns the authentication for the repository
func getAuth(conf *config.Config) *http.BasicAuth{
	username := conf.Adapter.SourceControl.Repository.Username
	if username != "" {
		accessToken := conf.Adapter.SourceControl.Repository.AccessToken
		return &http.BasicAuth{
			Username: username,
			Password: accessToken,
		}
	}
	return &http.BasicAuth{}
}

// fetchArtifacts clones the API artifacts from the remote repository into the artifacts directory in the adapter
func fetchArtifacts(conf *config.Config) (repository *git.Repository,err error) {
	artifactsDirName := filepath.FromSlash(conf.Adapter.SourceControl.ArtifactsDirectory + "/" + apisArtifactDir)
	repositoryURL := conf.Adapter.SourceControl.Repository.URL

	// Opens the local repository, if exists
	repository, _ = git.PlainOpen(artifactsDirName)

	// If a local repository exists, pull the changes from the remote repository
	if repository != nil {
		loggers.LoggerAPI.Info("Starting to fetch changes from remote repository")

		compareRepository(conf, repository)

		return repository, nil
	}

	// If a local repository does not exist, clone the remote repository
	loggers.LoggerAPI.Info("Fetching API artifacts from remote repository")

	// Clones the  remote repository
	repository, err = git.PlainClone(artifactsDirName, false, &git.CloneOptions{
		URL:  repositoryURL,
		Auth: getAuth(conf),
	})

	if err != nil {
		loggers.LoggerAPI.Error("Error while fetching artifacts from the remote repository ", err)
	}

	return repository, err

}

// pollChanges polls for changes from the remote repository
func pollChanges(conf *config.Config, repository *git.Repository){
	pollInterval := conf.Adapter.SourceControl.PollInterval
	for {
		<- time.After(time.Duration(pollInterval) * time.Second)
		go compareRepository(conf, repository)
	}
}

// compareRepository compares the hashes of the local and remote repositories and pulls if there are any changes
func compareRepository(conf *config.Config, localRepository *git.Repository){

	remote, err := localRepository.Remote("origin")
	if err != nil{
		loggers.LoggerAPI.Error("Error while returning remote. ", err)
	}

	remoteList, err := remote.List(&git.ListOptions{
		Auth: getAuth(conf),
	})
	if err != nil {
		loggers.LoggerAPI.Error("Error while listing remote. ", err)
	}

	ref, err := localRepository.Head()
	if err != nil {
		loggers.LoggerAPI.Error("Error while retrieving Head. ", err)
	}

	refName := ref.Name()

	localRepositoryHash := ref.Hash().String()

	for _, r := range remoteList {
		if r.Name() == refName && !r.Hash().IsZero() && localRepositoryHash != r.Hash().String() {
			loggers.LoggerAPI.Info("Fetching changes from remote repository")
			pullChanges(conf, localRepository)

			err := processArtifactChanges(conf)
			if err != nil {
				loggers.LoggerAPI.Error("Error while processing artifact changes. ", err)
			}

			//Redeploy changes
			artifactsMap, err = api.ProcessMountedAPIProjects()
			if err != nil {
				loggers.LoggerAPI.Error("Local api artifacts processing has failed.", err)
				return
			}
		}
	}

}

// pullChanges pulls changes from the given repository
func pullChanges(conf *config.Config, localRepository *git.Repository){
	workTree, err := localRepository.Worktree()
	if err != nil {
		loggers.LoggerAPI.Error("Error while retrieving the worktree. ", err)
	}

	err = workTree.Pull(&git.PullOptions{
		Auth: getAuth(conf),
	})
	if err != nil {
		loggers.LoggerAPI.Error("Error while pulling changes from repository. ", err)
	}
}

// processArtifactChanges undeploy the APIs whose artifacts are not present in the repository
func processArtifactChanges(conf *config.Config) (err error){
	apisDirName := filepath.FromSlash(conf.Adapter.SourceControl.ArtifactsDirectory + "/" + apisArtifactDir)
	files, err := ioutil.ReadDir(apisDirName)
	if err != nil {
		loggers.LoggerAPI.Error("Error while reading api artifacts during startup. ", err)
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
				return err
			}
		}
	}
	return nil
}


