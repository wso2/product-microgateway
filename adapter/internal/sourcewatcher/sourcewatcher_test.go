package sourcewatcher

import (
	"io/ioutil"
	"os"
	"testing"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/go-git/go-git/v5/plumbing/transport/http"
	"github.com/stretchr/testify/assert"
)

// Test the fetchArtifacts method for a public repository with invalid credentials
func TestFetchArtifactsWithInvalidCredentials(t *testing.T) {
	conf, _ := config.ReadConfigs()

	dir, err := ioutil.TempDir("", "test")

	defer os.RemoveAll(dir)

	if err != nil {
		t.Error("Error creating temp directory." + err.Error())
	}

	conf.Adapter.ArtifactsDirectory = dir
	conf.Adapter.SourceControl.ArtifactsDirectory = dir
	conf.Adapter.SourceControl.Repository.Username = "admin"
	conf.Adapter.SourceControl.Repository.AccessToken = "admin"
	conf.Adapter.SourceControl.Repository.URL = "https://github.com/wso2/product-microgateway"

	_, err = fetchArtifacts(conf)

	assert.NotNil(t, err, "Error fetching artifacts")
}

// Test the fetchArtifacts method for an invalid repository
func TestFetchArtifactsWithInvalidRepository(t *testing.T) {
	conf, _ := config.ReadConfigs()

	dir, err := ioutil.TempDir("", "test")

	defer os.RemoveAll(dir)

	if err != nil {
		t.Error("Error creating temp directory." + err.Error())
	}

	conf.Adapter.ArtifactsDirectory = dir
	conf.Adapter.SourceControl.ArtifactsDirectory = dir
	conf.Adapter.SourceControl.Repository.Username = ""
	conf.Adapter.SourceControl.Repository.AccessToken = ""
	conf.Adapter.SourceControl.Repository.URL = "https://github.com/user/repository"

	_, err = fetchArtifacts(conf)

	assert.NotNil(t, err, "Fetching artifacts failed")
}

// Test the getAuth method without credentials
func TestGetAuthWithoutCredentials(t *testing.T) {
	conf, _ := config.ReadConfigs()

	conf.Adapter.SourceControl.Repository.Username = ""
	conf.Adapter.SourceControl.Repository.AccessToken = ""

	auth := getAuth(conf)

	expectedAuth := &http.BasicAuth{}

	assert.Equal(t, expectedAuth, auth, "Invalid auth")
}

// Test the getAuth method with credentials
func TestGetAuthWithCredentials(t *testing.T) {
	conf, _ := config.ReadConfigs()

	conf.Adapter.SourceControl.Repository.Username = "admin"
	conf.Adapter.SourceControl.Repository.AccessToken = "admin"

	actualAuth := getAuth(conf)

	expectedAuth := &http.BasicAuth{
		Username: "admin",
		Password: "admin",
	}

	assert.Equal(t, expectedAuth, actualAuth, "Invalid auth")
}