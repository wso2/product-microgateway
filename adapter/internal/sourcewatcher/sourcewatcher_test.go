package sourcewatcher

import (
	"io/ioutil"
	"os"
	"testing"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/stretchr/testify/assert"
)

func setSourceWatcherConfig(artifactsDir, username, accessToken, repositoryURL string){

	conf, _ := config.ReadConfigs()

	conf.Adapter.ArtifactsDirectory = artifactsDir
	conf.Adapter.SourceControl.ArtifactsDirectory = artifactsDir
	conf.Adapter.SourceControl.Repository.Username = username
	conf.Adapter.SourceControl.Repository.AccessToken = username
	conf.Adapter.SourceControl.Repository.URL = repositoryURL

	config.SetConfig(conf)
}

// Test the fetchArtifacts method for a public repository with invalid credentials
func TestFetchArtifactsWithInvalidCredentials(t *testing.T) {

	dir, err := ioutil.TempDir("", "test")

	defer os.RemoveAll(dir)

	if err != nil {
		t.Error("Error creating temp directory." + err.Error())
	}

	setSourceWatcherConfig(dir, "admin", "admin", "https://github.com/wso2/product-microgateway")

	_, err = fetchArtifacts()

	assert.NotNil(t, err, "Error fetching artifacts")

	config.SetDefaultConfig()
}

// Test the fetchArtifacts method for an invalid repository
func TestFetchArtifactsWithInvalidRepository(t *testing.T) {

	dir, err := ioutil.TempDir("", "test")

	defer os.RemoveAll(dir)

	if err != nil {
		t.Error("Error creating temp directory." + err.Error())
	}

	setSourceWatcherConfig(dir, "admin", "admin", "https://github.com/user/repository")

	_, err = fetchArtifacts()

	assert.NotNil(t, err, "Fetching artifacts failed")

	config.SetDefaultConfig()
}
