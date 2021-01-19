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
package svcdiscovery

import (
	"errors"
	"fmt"
	"strings"
)

const (
	// ConsulBegin
	ConsulBegin string = "consul"
)

// DefaultHost host and port of the default host
// Clusters are initialized with default host at the time of initialization of an api project
type DefaultHost struct {
	Host string
	Port string
}

func IsDiscoveryServiceEndpoint(str string, discoveryServiceName string) bool {
	str = strings.TrimSpace(str)
	fmt.Print("IsDiscoveryServiceEndpoint ", strings.HasPrefix(str, discoveryServiceName))
	return strings.HasPrefix(str, discoveryServiceName)
}

func parseList(str string) []string {
	s := strings.Split(str, ",")
	for i := range s {
		s[i] = strings.TrimSpace(strings.ReplaceAll(s[i], "[", ""))
		s[i] = strings.TrimSpace(strings.ReplaceAll(s[i], "]", ""))
		if strings.TrimSpace(s[i]) == "*" {
			s[i] = ""
		}
	}
	return s
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
	str = strings.Replace(str, ConsulBegin, "", 1)
	str = strings.TrimSpace(str)
	return str, defaultHost, nil
}

//ParseQueryString parses the string into a QueryString struct
func ParseQueryString(query string) (Query, error) {
	//example-->
	//consul:[dc1,dc2].namespace.serviceA.[tag1,tag2]
	//if !IsDiscoveryServiceEndpoint(query, ConsulBegin) {
	//	return QueryString{}, errors.New("not a consul service string")
	//}
	//split := strings.Split(query, ":")
	//if len(split) != 2 {
	//	return QueryString{}, errors.New("bad query syntax")
	//}
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
	return Query{}, errors.New("bad query syntax")
}

//func getDefaultHost(rawURL string) (DefaultHost, error) {
//	if !strings.Contains(rawURL, "://") {
//		rawURL = "http://" + rawURL
//	}
//	val, err1 := url.Parse(rawURL)
//
//	defHost := DefaultHost{
//		Host: "",
//		Port: "",
//	}
//	//try with SplitHostPort
//	if err1 != nil {
//		h, p, err2 := net.SplitHostPort(rawURL)
//		if err2 != nil {
//			//try with ParseIP
//			ip := net.ParseIP(rawURL)
//			if ip != nil {
//				defHost.Host = ip.String()
//			}
//		} else {
//			defHost.Host = h
//			defHost.Port = p
//		}
//	} else {
//		if val != nil {
//			defHost.Host = val.Hostname()
//			defHost.Port = val.Port()
//		}
//	}
//
//	return defHost, nil
//}
