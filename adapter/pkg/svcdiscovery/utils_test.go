package svcdiscovery

import (
	"errors"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestIsDiscoveryServiceEndpoint(t *testing.T) {
	type isDiscoveryServiceEndpointList struct {
		input   string
		output  bool
		message string
	}
	dataItems := []isDiscoveryServiceEndpointList{
		{
			input:   "consul:",
			output:  true,
			message: "only consul keyword",
		},
		{
			input:   "consul:",
			output:  true,
			message: "only ConsulBegin keyword",
		},
		{
			input:   "consul:[dc1,dc2].dev.serviceA.[tag1,tag2]",
			output:  true,
			message: "valid",
		},
		{
			input:   "",
			output:  false,
			message: "empty",
		},
		{
			input:   "consul",
			output:  false,
			message: "without :",
		},
	}

	for i, item := range dataItems {
		result := IsDiscoveryServiceEndpoint(item.input, ConsulBegin)
		assert.Equal(t, item.output, result, item.message, i)
	}
}

func TestParseSyntax(t *testing.T) {
	type parseListTestItem struct {
		input   string
		result  QueryString
		err     error
		message string
	}
	dataItems := []parseListTestItem{
		{
			input: "consul:[dc1,dc2].dev.serviceA.[tag1,tag2];http://abc.com:80",
			result: QueryString{
				Datacenters: []string{"dc1", "dc2"},
				ServiceName: "serviceA",
				Namespace:   "dev",
				Tags:        []string{"tag1", "tag2"},
			},
			err:     nil,
			message: "simple scenario with namespace",
		},
		{
			input: "consul:[dc 1,dc 2].service A.[tag1,tag2];http://192.168.0.1:3000",
			result: QueryString{
				Datacenters: []string{"dc 1", "dc 2"},
				ServiceName: "service A",
				Namespace:   "",
				Tags:        []string{"tag1", "tag2"},
			},
			err:     nil,
			message: "simple scenario without namespace",
		},
		{
			input: "consul:[].prod.serviceA.[*];http://abc.com:80",
			result: QueryString{
				Datacenters: []string{""},
				ServiceName: "serviceA",
				Namespace:   "prod",
				Tags:        []string{""},
			},
			err:     nil,
			message: "empty dcs and tags",
		},
		{
			input:   "consul[].prod.serviceA.[*]",
			err:     errors.New("default host not provided"),
			message: "empty dcs and tags",
		},
		{
			input:   "consul:[].fake.another.prod.serviceA.[*];http://abc.com:80",
			err:     errors.New("bad query syntax"),
			message: "5 pieces in syntax",
		},
	}
	for i, item := range dataItems {
		result, _, err := ParseConsulSyntax(item.input)
		assert.Equal(t, item.result, result, item.message, i)
		assert.Equal(t, item.err, err, item.message)
	}
}

func TestParseList(t *testing.T) {
	type parseListTestItem struct {
		inputString string
		resultList  []string
		message     string
	}
	dataItems := []parseListTestItem{
		{
			inputString: "[dc1,dc2,aws-us-central-1]",
			resultList:  []string{"dc1", "dc2", "aws-us-central-1"},
			message:     "Simple scenario with 3dcs",
		},
		{
			inputString: "[]",
			resultList:  []string{""},
			message:     "Empty list :(all)",
		},
		{
			inputString: "[*]",
			resultList:  []string{""},
			message:     "Empty list with * :(all)",
		},
		{
			inputString: "[abc]",
			resultList:  []string{"abc"},
			message:     "List with one dc",
		},
	}
	for _, item := range dataItems {
		result := parseList(item.inputString)
		assert.Equal(t, item.resultList, result, item.message)
	}
}

//func TestCleanString(t *testing.T) {
//	type cleanStringTestItem struct {
//		inputString string
//		result      string
//		message     string
//	}
//	dataItems := []cleanStringTestItem{
//		{
//			inputString: "consul:[dc 1,dc 2].service A.[tag1,tag2]",
//			result:      "consuldc1dc2serviceAtag1tag2",
//			message:     "[ ] , <whitespace>",
//		},
//	}
//	for _, item := range dataItems {
//		result := cleanString(item.inputString)
//		assert.Equal(t, item.result, result, item.message)
//	}
//
//}

func TestGetDefaultHost(t *testing.T) {
	type getDefaultHostTestItem struct {
		inputString string
		result      DefaultHost
		message     string
	}

	dataItems := []getDefaultHostTestItem{
		{
			inputString: "",
			result: DefaultHost{
				Host: "",
				Port: "",
			},
			message: "empty string",
		},
		{
			inputString: "http://www.dumpsters.com",
			result: DefaultHost{
				Host: "www.dumpsters.com",
				Port: "",
			},
			message: "url with http and www",
		},
		{
			inputString: "https://www.dumpsters.com:443",
			result: DefaultHost{
				Host: "www.dumpsters.com",
				Port: "443",
			},
			message: "url with port",
		},
		{
			inputString: "testing-path.com",
			result: DefaultHost{
				Host: "testing-path.com",
				Port: "",
			},
			message: "url without http",
		},
		{
			inputString: "abc.com:80",
			result: DefaultHost{
				Host: "abc.com",
				Port: "80",
			},
			message: "url +port without http ",
		},
		{
			inputString: "http://abc.com:80",
			result: DefaultHost{
				Host: "abc.com",
				Port: "80",
			},
			message: "url +port +http ",
		},
		{
			inputString: "192.168.0.1",
			result: DefaultHost{
				Host: "192.168.0.1",
				Port: "",
			},
			message: "ipv4",
		},
		{
			inputString: "192.168.0.1:80",
			result: DefaultHost{
				Host: "192.168.0.1",
				Port: "80",
			},
			message: "ipv4+port",
		},
		{
			inputString: "http://192.168.0.1:80",
			result: DefaultHost{
				Host: "192.168.0.1",
				Port: "80",
			},
			message: "ipv4+port+http",
		},
		{
			inputString: "http://2402:4000:2081:3573:e04f:da63:e607:d34d",
			result: DefaultHost{
				Host: "2402:4000:2081:3573:e04f:da63:e607:d34d",
				Port: "",
			},
			message: "ipv6+http",
		},
		{
			inputString: "::1",
			result: DefaultHost{
				Host: "::1",
				Port: "",
			},
			message: "ipv6 shorthand",
		},
		{
			inputString: "2001:4860:0:2001::68",
			result: DefaultHost{
				Host: "2001:4860:0:2001::68",
				Port: "",
			},
			message: "ipv6",
		}, {
			inputString: "[1fff:0:a88:85a3::ac1f]:8001",
			result: DefaultHost{
				Host: "1fff:0:a88:85a3::ac1f",
				Port: "8001",
			},
			message: "ipv6+port",
		}, {
			inputString: "https://[1fff:0:a88:85a3::ac1f]:8001",
			result: DefaultHost{
				Host: "1fff:0:a88:85a3::ac1f",
				Port: "8001",
			},
			message: "ipv6+port+http",
		},
	}

	for _, item := range dataItems {
		_, _ = getDefaultHost(item.inputString)
		//assert.Equal(t, item.result, result, item.message)
	}

}
