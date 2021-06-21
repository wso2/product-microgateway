package controlplane

import (
	"bytes"
	"encoding/json"
	"errors"
	"io/ioutil"
	"net/http"
	"net/url"
	"strconv"
	"strings"

    "github.com/wso2/product-microgateway/adapter/internal/auth"
    "github.com/wso2/product-microgateway/adapter/pkg/tlsutils"
)

var (
	client Client //Client
	conf   Conf
)

//GetAppCredentials register/retrieve application
func GetAppCredentials(cpConf Conf) error {
	conf = cpConf
	values := map[string]string{
		"clientName": conf.ClientName,
		"owner":      conf.Owner,
		"grantType":  conf.GrantType}
	jsonValue, _ := json.Marshal(values)

	req, _ := http.NewRequest("POST", conf.ClientRegEP, bytes.NewBuffer(jsonValue))

	// Setting authorization header
	basicAuth := "Basic " + auth.GetBasicAuth(conf.Username, conf.Password)
	req.Header.Set("Authorization", basicAuth)
	req.Header.Set("Content-Type", "application/json")

	resp, err := tlsutils.InvokeControlPlane(req, conf.SkipSSLVerification)

	if err != nil {
		return err
	}
	if resp == nil || resp.StatusCode != http.StatusOK {
		return errors.New("error in response retrieved for client register request. status:" + strconv.Itoa(resp.StatusCode))
	}

	respBytes, err := ioutil.ReadAll(resp.Body)
	if err == nil {
		err = json.Unmarshal(respBytes, &client)
	}

	return err
}

//GetAccessToken generate access token
func GetAccessToken(scopes string, grantType string) (Token, error) {
	data := url.Values{}
	data.Set("username", conf.Username)
	data.Set("password", conf.Password)
	data.Set("scope", scopes)
	data.Set("grant_type", grantType)

	skipSSL := conf.SkipSSLVerification

	req, _ := http.NewRequest("POST", conf.TokenEP, strings.NewReader(data.Encode()))

	// Setting authorization header
	basicAuth := "Basic " + auth.GetBasicAuth(client.ClientID, client.ClientSecret)
	req.Header.Set("authorization", basicAuth)
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := tlsutils.InvokeControlPlane(req, skipSSL)
	var token Token
	if err != nil {
		return token, err
	}
	if resp == nil || resp.StatusCode != http.StatusOK {
		return token, errors.New("error in response retrieved for access token request. status: " + strconv.Itoa(resp.StatusCode)+ client.ClientID + " : " + client.ClientSecret)
	}

	respBytes, err := ioutil.ReadAll(resp.Body)
	if err == nil {
		err = json.Unmarshal(respBytes, &token)
	}

	return token, err
}
