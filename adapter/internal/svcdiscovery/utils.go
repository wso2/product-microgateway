/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package svcdiscovery

import (
	"errors"
	"regexp"
	"strings"
)

const (
	// consulBegin
	consulBegin string = "consul"
)

// DefaultHost host and port of the default host
// Clusters are initialized with default host at the time of initialization of an api project
type DefaultHost struct {
	Host string
	Port string
}

//IsDiscoveryServiceEndpoint checks whether an endpoint string is a consul syntax string
func IsDiscoveryServiceEndpoint(str string) bool {
	str = strings.TrimSpace(str)
	re, _ := regexp.Compile(`^consul(\s*)\(.*,.*\)$`)
	return re.MatchString(str)
}

//parse a list of datacenters or tags
func parseList(str string) []string {
	parsedString := strings.Split(str, ",")
	for i := range parsedString {
		parsedString[i] = strings.TrimSpace(strings.ReplaceAll(parsedString[i], "[", ""))
		parsedString[i] = strings.TrimSpace(strings.ReplaceAll(parsedString[i], "]", ""))
		if strings.TrimSpace(parsedString[i]) == "*" {
			parsedString[i] = ""
		}
	}
	return parsedString
}

//ParseConsulSyntax breaks the syntax string into query string and default host string
func ParseConsulSyntax(str string) (string, string, error) {
	list := strings.Split(str, ",")
	length := len(list)
	if length < 2 {
		return "", "", errors.New("default host not provided")
	}
	defaultHost := list[length-1]
	defaultHost = strings.Replace(defaultHost, ")", "", 1)
	defaultHost = strings.TrimSpace(defaultHost)

	for i := 0; i < length-1; i++ {
		str += list[i]
		str = strings.Join(list[0:length-1], ",")
	}
	str = strings.Replace(str, "(", "", 1)
	str = strings.Replace(str, consulBegin, "", 1)
	str = strings.TrimSpace(str)
	return str, defaultHost, nil
}

//ParseQueryString parses the string into a Query struct
func ParseQueryString(query string) (Query, error) {
	//examples-->
	//[dc1,dc2].namespace.serviceA.[tag1,tag2]
	//dc1.serviceA.tag1
	//serviceA
	//
	str := strings.Split(query, ".")
	qCategory := len(str)
	if qCategory == 1 { //service name only
		queryString := Query{
			Datacenters: parseList("*"),
			ServiceName: strings.TrimSpace(str[0]),
			Namespace:   "",
			Tags:        parseList("*"),
		}
		return queryString, nil
	} else if qCategory == 3 { //datacenters, service name, tags
		queryString := Query{
			Datacenters: parseList(str[0]),
			ServiceName: strings.TrimSpace(str[1]),
			Namespace:   "",
			Tags:        parseList(str[2]),
		}
		return queryString, nil
	} else if qCategory == 4 { //datacenters, namespace, service name, tags
		queryString := Query{
			Datacenters: parseList(str[0]),
			ServiceName: strings.TrimSpace(str[2]),
			Namespace:   strings.TrimSpace(str[1]),
			Tags:        parseList(str[3]),
		}
		return queryString, nil
	}
	return Query{}, errors.New("bad consul query syntax")
}
