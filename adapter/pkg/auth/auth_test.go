package auth

import (
	"testing"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/go-git/go-git/v5/plumbing/transport/http"
	"github.com/stretchr/testify/assert"
)

func setGitAuth(username, accessToken, sshKeyFile string){
	conf, _ := config.ReadConfigs()

	conf.Adapter.SourceControl.Repository.Username = username
	conf.Adapter.SourceControl.Repository.AccessToken = accessToken
	conf.Adapter.SourceControl.Repository.SSHKeyFile = sshKeyFile

	config.SetConfig(conf)
}

// Test the getAuth method without credentials
func TestGetGitAuthWithoutCredentials(t *testing.T) {
	setGitAuth("", "", "")

	auth, _ := GetGitAuth()

	expectedAuth := &http.BasicAuth{}

	assert.Equal(t, expectedAuth, auth, "Invalid auth")

	config.SetDefaultConfig()
}

// Test the getAuth method with credentials
func TestGetAuthWithCredentials(t *testing.T) {
	setGitAuth("admin", "admin", "")

	actualAuth, _ := GetGitAuth()

	expectedAuth := &http.BasicAuth{
		Username: "admin",
		Password: "admin",
	}

	assert.Equal(t, expectedAuth, actualAuth, "Invalid auth")

	config.SetDefaultConfig()
}
