package utills

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/ghodss/yaml"
	"unicode"
)

// ToJSON converts a single YAML document into a JSON document
// or returns an error. If the document appears to be JSON the
// YAML decoding path is not used.
func ToJSON(data []byte) ([]byte, error) {
	if hasJSONPrefix(data) {
		return data, nil
	}
	return yaml.YAMLToJSON(data)
}

var jsonPrefix = []byte("{")

// hasJSONPrefix returns true if the provided buffer appears to start with
// a JSON open brace.
func hasJSONPrefix(buf []byte) bool {
	return hasPrefix(buf, jsonPrefix)
}

// Return true if the first non-whitespace bytes in buf is prefix.
func hasPrefix(buf []byte, prefix []byte) bool {
	trim := bytes.TrimLeftFunc(buf, unicode.IsSpace)
	return bytes.HasPrefix(trim, prefix)
}

func FindSwaggerVersion(jsn []byte) (string, error) {
	var version string = "3"
	var result map[string]interface{}

	err := json.Unmarshal(jsn, &result)
	if err != nil {
		fmt.Printf("json unmarsheliing err when finding the swaggerVersion : %v\n", err)
	}

	if _, ok := result["swagger"]; ok {
		version = "2"
	} else if _, ok := result["openapi"]; ok {
		version = "3"
	} else {
		return version, errors.New("swagger file version is not defined. Default version assigned to 3 ")
	}

	return version, nil
}
