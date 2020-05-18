// Artifacts.go will retrive the data of the artifacts related to the filter chains.
// For example it could be API definitions, configuration files related to filters.

package controller

import (
	"io/ioutil"

	"errors"
	"fmt"
	"net/url"

	oapi3 "github.com/getkin/kin-openapi/openapi3"
	log "github.com/sirupsen/logrus"
)

// This function will read the API definition from the mounted location.
// Input parameters
//	- file location of the file to be read
// Return
//	- []byte of the read API definition.
//	- error if occurred when reading the file
func readFile(file string) ([]byte, error) {
	cont, err := ioutil.ReadFile(file)
	//if reading fails
	if err != nil {
		log.Warnf("Error in reading the file %v: error - %v", file, err)
	}

	return cont, err
}

// This function will read the API definition resides in <home>/artifacts/apis location
// Input parameters
// - None
// Return parameters
// - openAPI swagger definitions array, error if occurred
//todo: return map
func readApis() ([]oapi3.Swagger, error) {

	//Reading the files in the API directory
	ff, err := ioutil.ReadDir("./filter-chain/artifacts/apis/")

	//if reading directory fails,
	if err != nil {
		log.Warnf("Error while reading the directory - %v", err)
		return nil, err
	}
	var apis = make([]oapi3.Swagger, len(ff))
	for i, f := range ff {
		cont, err := readFile("./filter-chain/artifacts/apis/" + f.Name())
		if err != nil {
			//Handle error
		}
		swagger, err := oapi3.NewSwaggerLoader().LoadSwaggerFromData(cont)
		apis[i] = *swagger
	}
	return apis, nil
}

// ResolveBasepaths method identify the basepaths from the APIs attached to the filterchain
// It would create a map containing API name as the key  and the basepath as the value
// Input parameters
// - swagger definitions : which are attached to the filter chain
// Return parameters
// - map[string]string containing API names and base-paths/contexts
func ResolveBasepaths(apis []oapi3.Swagger) (map[string]string, error) {
	//server url templating can have urls similar to 'https://{customerId}.saas-app.com:{port}/v2'

	apidetails := make(map[string]string)
	for _, api := range apis {
		server := api.Servers[0]
		basepath := api.ExtensionProps.Extensions["x-wso2-basepath"]
		var context string
		name := api.Info.Title

		if basepath != nil {
			context = fmt.Sprintf("%v", basepath)
		} else if server != nil {
			u, err := url.Parse(server.URL)
			if err != nil {
				log.Error("Error occurred while passing the URL of the server")
				return nil, err
			}
			//get the path as the basepath or context
			context = u.Path
		} else {
			// if server section and the basepath vendor extension is not present.
			// then the context would b "/"
			return nil, errors.New("x-wso2-basepath or servers extensions not present in the swagger")
		}
		apidetails[name] = context
	}
	return apidetails, nil
}

// GetAPI function returns the API definition of the invoking API call
// Input parameters
// - name of the API
// - API definitions
// Output parameters
// - API definition for the given name
// - error if not find the API
func GetAPI(name string, apis []oapi3.Swagger) (oapi3.Swagger, error) {
	for _, api := range apis {
		if api.Info.Title == name {
			return api, nil
		}
	}
	var r *oapi3.Swagger
	return *r, nil
}
