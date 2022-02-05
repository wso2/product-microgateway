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
 */

package model

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"regexp"
	"strings"
	"text/template"

	"gopkg.in/yaml.v2"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"
)

const (
	openAPIDir                     string = "Definitions"
	openAPIFilename                string = "swagger."
	apiYAMLFile                    string = "api.yaml"
	deploymentsYAMLFile            string = "deployment_environments.yaml"
	endpointCertFile               string = "endpoint_certificates."
	apiJSONFile                    string = "api.json"
	endpointCertDir                string = "Endpoint-certificates"
	interceptorCertDir             string = "Endpoint-certificates/interceptors"
	policiesDir                    string = "Policies"
	policySpecFileExtension        string = ".yaml"
	policyDefFileExtension         string = ".gotmpl"
	policyCCGateway                string = "CC"
	crtExtension                   string = ".crt"
	pemExtension                   string = ".pem"
	apiTypeFilterKey               string = "type"
	apiTypeYamlKey                 string = "type"
	lifeCycleStatus                string = "lifeCycleStatus"
	securityScheme                 string = "securityScheme"
	endpointImplementationType     string = "endpointImplementationType"
	prototypedImplementationStatus string = "prototyped"
	inlineEndpointType             string = "INLINE"
	templateEndpointType           string = "TEMPLATE"
	endpointSecurity               string = "endpoint_security"
	production                     string = "production"
	sandbox                        string = "sandbox"
	zipExt                         string = ".zip"
)

const (
	policyValTypeString string = "String"
	policyValTypeInt    string = "Integer"
	policyValTypeBool   string = "Boolean" // TODO: check type names with APIM
)

type PolicyFlow string

var (
	PolicyInFlow    PolicyFlow = "request"
	PolicyOutFlow   PolicyFlow = "response"
	PolicyFaultFlow PolicyFlow = "fault"
)

// ProjectAPI contains the extracted from an API project zip
type ProjectAPI struct {
	APIYaml            APIYaml
	APIEnvProps        map[string]synchronizer.APIEnvProps
	Deployments        []Deployment
	OpenAPIJsn         []byte
	InterceptorCerts   []byte
	APIType            string                     // read from api.yaml and formatted to upper case
	APILifeCycleStatus string                     // read from api.yaml and formatted to upper case
	OrganizationID     string                     // read from api.yaml or config
	Policies           map[string]PolicyContainer // read from policy dir

	//UpstreamCerts cert filename -> cert bytes
	UpstreamCerts map[string][]byte
	//EndpointCerts url -> cert filename
	EndpointCerts map[string]string
}

// EndpointSecurity contains parameters of endpoint security at api.json
type EndpointSecurity struct {
	Password         string            `json:"password,omitempty" mapstructure:"password"`
	Type             string            `json:"type,omitempty" mapstructure:"type"`
	Enabled          bool              `json:"enabled,omitempty" mapstructure:"enabled"`
	Username         string            `json:"username,omitempty" mapstructure:"username"`
	CustomParameters map[string]string `json:"customparameters,omitempty" mapstructure:"customparameters"`
}

// APIEndpointSecurity represents the structure of endpoint_security param in api.yaml
type APIEndpointSecurity struct {
	Production EndpointSecurity `json:"production,omitempty"`
	Sandbox    EndpointSecurity `json:"sandbox,omitempty"`
}

// ApimMeta represents APIM meta information of files received from APIM
type ApimMeta struct {
	Type    string `yaml:"type" json:"type"`
	Version string `yaml:"version" json:"version"`
}

// DeploymentEnvironments represents content of deployment_environments.yaml file
// of an API_CTL Project
type DeploymentEnvironments struct {
	ApimMeta
	Data []Deployment `yaml:"data"`
}

// Deployment represents deployment information of an API_CTL project
type Deployment struct {
	DisplayOnDevportal    bool   `yaml:"displayOnDevportal"`
	DeploymentVhost       string `yaml:"deploymentVhost"`
	DeploymentEnvironment string `yaml:"deploymentEnvironment"`
}

// EndpointCertificatesDetails represents content of endpoint_certificates.yaml file
// of an API_CTL Project
type EndpointCertificatesDetails struct {
	ApimMeta
	Data []EndpointCertificate `json:"data"`
}

// EndpointCertificate represents certificate information of an API_CTL project
type EndpointCertificate struct {
	Alias       string `json:"alias"`
	Endpoint    string `json:"endpoint"`
	Certificate string `json:"certificate"`
}

// APIYaml contains everything necessary to extract api.json/api.yaml file
// To support both api.json and api.yaml we convert yaml to json and then use json.Unmarshal()
// Therefore, the params are defined to support json.Unmarshal()
type APIYaml struct {
	ApimMeta
	Data struct {
		ID                         string   `json:"Id,omitempty"`
		Name                       string   `json:"name,omitempty"`
		Context                    string   `json:"context,omitempty"`
		Version                    string   `json:"version,omitempty"`
		RevisionID                 int      `json:"revisionId,omitempty"`
		APIType                    string   `json:"type,omitempty"`
		LifeCycleStatus            string   `json:"lifeCycleStatus,omitempty"`
		EndpointImplementationType string   `json:"endpointImplementationType,omitempty"`
		AuthorizationHeader        string   `json:"authorizationHeader,omitempty"`
		SecurityScheme             []string `json:"securityScheme,omitempty"`
		OrganizationID             string   `json:"organizationId,omitempty"`
		EndpointConfig             struct {
			EndpointType                 string              `json:"endpoint_type,omitempty"`
			LoadBalanceAlgo              string              `json:"algoCombo,omitempty"`
			LoadBalanceSessionManagement string              `json:"sessionManagement,omitempty"`
			LoadBalanceSessionTimeOut    string              `json:"sessionTimeOut,omitempty"`
			APIEndpointSecurity          APIEndpointSecurity `json:"endpoint_security,omitempty"`
			RawProdEndpoints             interface{}         `json:"production_endpoints,omitempty"`
			ProductionEndpoints          []EndpointInfo
			ProductionFailoverEndpoints  []EndpointInfo `json:"production_failovers,omitempty"`
			RawSandboxEndpoints          interface{}    `json:"sandbox_endpoints,omitempty"`
			SandBoxEndpoints             []EndpointInfo
			SandboxFailoverEndpoints     []EndpointInfo `json:"sandbox_failovers,omitempty"`
			ImplementationStatus         string         `json:"implementation_status,omitempty"`
		} `json:"endpointConfig,omitempty"`
		Operations []OperationYaml `json:"Operations,omitempty"`
	} `json:"data"`
}

// OperationYaml holds attributes of APIM operations
type OperationYaml struct {
	Target            string            `json:"target,omitempty"`
	Verb              string            `json:"verb,omitempty"`
	OperationPolicies OperationPolicies `json:"operationPolicies,omitempty"`
}

// OperationPolicies holds policies of the APIM operations
type OperationPolicies struct {
	In    []Policy `json:"in,omitempty"`
	Out   []Policy `json:"out,omitempty"`
	Fault []Policy `json:"fault,omitempty"`
}

// Policy holds APIM policies
type Policy struct {
	PolicyName   string      `json:"policyName,omitempty"`
	TemplateName string      `json:"_,omitempty"`
	Order        int         `json:"order,omitempty"`
	Parameters   interface{} `json:"parameters,omitempty"`
	isIncluded   bool        `json:"-"` // used to check whether multiple instance of policy applied
}

// PolicyContainer holds the definition and specification of policy
type PolicyContainer struct {
	Specification PolicySpecification
	Definition    PolicyDefinition
}

// PolicySpecification holds policy specification from ./Policy/<policy>.yaml files
type PolicySpecification struct {
	ApimMeta
	Data struct { // TODO: check all fields are required or omit empty in APIM side
		Name              string   `yaml:"name"`
		ApplicableFlows   []string `yaml:"applicableFlows"`
		SupportedGateways []string `yaml:"supportedGateways"`
		SupportedApiTypes []string `yaml:"supportedApiTypes"`
		MultipleAllowed   bool     `yaml:"multipleAllowed"`
		PolicyAttributes  []struct {
			Name            string `yaml:"name"`
			ValidationRegex string `yaml:"validationRegex,omitempty"`
			Type            string `yaml:"type"`
			Required        bool   `yaml:"required,omitempty"`
		} `yaml:"policyAttributes"`
	}
}

// PolicyDefinition holds the content of policy definition which is rendered from ./Policy/<policy>.gotmpl files
type PolicyDefinition struct {
	ApimMeta
	Data struct {
		Action     string
		Parameters map[string]string
	}
	RawData []byte `yaml:"_"`
}

// EndpointInfo holds config values regards to the endpoint
type EndpointInfo struct {
	Endpoint string `json:"url,omitempty"`
	Config   struct {
		ActionDuration string `json:"actionDuration,omitempty"`
		RetryTimeOut   string `json:"retryTimeOut,omitempty"`
	} `json:"config,omitempty"`
}

// validatePolicy validates the given policy against the spec
func (spec *PolicySpecification) validatePolicy(policyList []Policy, pIndex int, flow PolicyFlow) error {
	policy := policyList[pIndex]
	if spec.Data.Name != policy.PolicyName {
		return fmt.Errorf("invalid policy specification, spec name %s and policy name %s mismatch", spec.Data.Name, policy.PolicyName)
	}
	// TODO: check ApplicableFlows are (in, out, fault) or (request, response, fault)
	if !arrayContains(spec.Data.ApplicableFlows, string(flow)) {
		return fmt.Errorf("policy flow \"%s\" not supported", flow)
	}
	if !arrayContains(spec.Data.SupportedGateways, policyCCGateway) {
		return errors.New("choreo connect gateway not supported")
	}
	if !spec.Data.MultipleAllowed {
		count := 0
		for i, p := range policyList {
			if p.PolicyName == spec.Data.Name {
				count += 1
				// TODO: check the behaviour sith APIM
				// in here allow first instance of policy to be applied if multiple is found
				if count > 1 {
					if pIndex >= i {
						return errors.New("multiple policies not allowed")
					}
					loggers.LoggerAPI.Warnf("Operation policy \"%v\" not allowed in multiple times, appling the first policy", policy.PolicyName)
					break
				}
			}
		}
	}

	policyPrams, ok := policy.Parameters.(map[string]interface{}) // TODO: check if we can change prams to map so no need this check
	if ok {
		for _, attrib := range spec.Data.PolicyAttributes {
			val, found := policyPrams[attrib.Name]
			if attrib.Required && !found {
				return fmt.Errorf("required paramater %s not found", attrib.Name)
			}

			switch v := val.(type) {
			case string:
				if !strings.EqualFold(attrib.Type, policyValTypeString) {
					return fmt.Errorf("invalid value type of paramater %s, required %s", attrib.Name, attrib.Type)
				}
				if attrib.ValidationRegex != "" {
					regexStr := strings.Trim(attrib.ValidationRegex, "/")
					reg, err := regexp.Compile(regexStr)
					if err != nil {
						return fmt.Errorf("invalid regex expression in policy spec %s, regex: \"%s\"", spec.Data.Name, attrib.ValidationRegex)
					}
					if !reg.MatchString(v) {
						return fmt.Errorf("invalid parameter value of attribute \"%s\", regex match failed", attrib.Name)
					}
				}
			case int:
				if !strings.EqualFold(attrib.Type, policyValTypeInt) {
					return fmt.Errorf("invalid value type of paramater %s, required %s", attrib.Name, attrib.Type)
				}
			case bool:
				if !strings.EqualFold(attrib.Type, policyValTypeBool) {
					return fmt.Errorf("invalid value type of paramater %s, required %s", attrib.Name, attrib.Type)
				}
			default:
				return fmt.Errorf("invalid value type of paramater %s, unsupported type %s", attrib.Name, attrib.Type)
			}
		}
	}

	return nil
}

// ValidateAPIType checks if the apiProject is properly assigned with the type.
func (apiProject *ProjectAPI) ValidateAPIType() error {
	var err error
	if apiProject.APIYaml.Type == "" {
		// If no api.yaml file is included in the zip folder, return with error.
		err = errors.New("could not find api.yaml or api.json")
		return err
	} else if apiProject.APIType != constants.HTTP && apiProject.APIType != constants.WS && apiProject.APIType != constants.WEBHOOK {
		errMsg := "API type is not currently supported with Choreo Connect"
		err = errors.New(errMsg)
		return err
	}
	return nil
}

// getFormattedPolicyFromTemplated returns formatted, Choreo Connect policy from a user templated policy
func (apiProject *ProjectAPI) getFormattedOperationalPolicies(policies OperationPolicies) OperationPolicies {
	fmtPolicies := OperationPolicies{}

	for i := range policies.In {
		// using index i instead of struct to validate policy against multiple allowed and apply only first if multiple exists
		if fmtPolicy, err := apiProject.getFormattedPolicyFromTemplated(policies.In, i, PolicyInFlow); err == nil {
			fmtPolicies.In = append(fmtPolicies.In, fmtPolicy)
		}
	}

	for i := range policies.Out {
		if fmtPolicy, err := apiProject.getFormattedPolicyFromTemplated(policies.Out, i, PolicyOutFlow); err == nil {
			fmtPolicies.Out = append(fmtPolicies.In, fmtPolicy)
		}
	}

	for i := range policies.Fault {
		if fmtPolicy, err := apiProject.getFormattedPolicyFromTemplated(policies.Fault, i, PolicyFaultFlow); err == nil {
			fmtPolicies.Fault = append(fmtPolicies.In, fmtPolicy)
		}
	}

	return fmtPolicies
}

// getFormattedPolicyFromTemplated returns formatted, Choreo Connect policy from a user templated policy
func (apiProject *ProjectAPI) getFormattedPolicyFromTemplated(policyList []Policy, pIndex int, flow PolicyFlow) (Policy, error) {
	// using index i instead of struct to validate policy against multiple allowed and apply only first if multiple exists
	policy := policyList[pIndex]
	spec := apiProject.Policies[policy.PolicyName].Specification
	if err := spec.validatePolicy(policyList, pIndex, flow); err != nil {
		loggers.LoggerAPI.Errorf("Operation policy validation failed, ignoring the policy \"%v\": %v", policy.PolicyName, err)
		return policy, err
	}

	defRaw := apiProject.Policies[policy.PolicyName].Definition.RawData
	t, err := template.New("policy-def").Parse(string(defRaw))
	if err != nil {
		loggers.LoggerAPI.Errorf("Error parsing the operation policy definition \"%v\" into go template: %v", policy.PolicyName, err)
		return Policy{}, err
	}

	var out bytes.Buffer
	err = t.Execute(&out, policy.Parameters)
	if err != nil {
		loggers.LoggerAPI.Errorf("Error operation policy definition \"%v\": %v", policy.PolicyName, err)
		return Policy{}, err
	}

	def := PolicyDefinition{}
	if err := yaml.Unmarshal(out.Bytes(), &def); err != nil {
		loggers.LoggerAPI.Errorf("Error parsing standardized operation policy definition \"%v\" into yaml: %v", policy.PolicyName, err)
		return Policy{}, err
	}

	// update templated policy itself and return, not updating a pointer to keep the original template values as it is.
	policy.Parameters = def.Data.Parameters
	policy.TemplateName = def.Data.Action

	return policy, nil
}

// StandardizeAPIYamlOperationPolicies updates API yaml with the CC standards by removing user defined templates.
func (apiProject *ProjectAPI) StandardizeAPIYamlOperationPolicies() error {
	for i, operation := range apiProject.APIYaml.Data.Operations {
		for j, policy := range operation.OperationPolicies.In {
			userP := policy.Parameters
			data := apiProject.Policies[policy.PolicyName].Definition.RawData
			t, err := template.New("policy-def").Parse(string(data))
			if err != nil {
				loggers.LoggerAPI.Errorf("Error parsing the operation policy definition %v into go template: %v", policy.PolicyName, err)
				continue
			}

			var out bytes.Buffer
			err = t.Execute(&out, userP)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error operation policy definition %v: %v", policy.PolicyName, err)
				continue
			}

			def := PolicyDefinition{}
			if err := yaml.Unmarshal(out.Bytes(), &def); err != nil {
				loggers.LoggerAPI.Errorf("Error parsing standardized operation policy definition %v into yaml: %v", policy.PolicyName, err)
				continue
			}

			// update API yaml file with choreo connect standadize parameter names
			policy.Parameters = def.Data.Parameters
			policy.TemplateName = def.Data.Action
			operation.OperationPolicies.In[j] = policy
			apiProject.APIYaml.Data.Operations[i] = operation
		}
	}

	return nil
}

// ProcessFilesInsideProject process single file inside API Project and update the apiProject instance appropriately.
func (apiProject *ProjectAPI) ProcessFilesInsideProject(fileContent []byte, fileName string) (err error) {
	newLineByteArray := []byte("\n")
	if strings.Contains(fileName, deploymentsYAMLFile) {
		loggers.LoggerAPI.Debug("Setting deployments of API")
		deployments, err := parseDeployments(fileContent)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error occurred while parsing the deployment environments: %v %v",
				fileName, err.Error())
		}
		apiProject.Deployments = deployments
	}
	if strings.Contains(fileName, openAPIDir+string(os.PathSeparator)+openAPIFilename) {
		loggers.LoggerAPI.Debugf("openAPI file : %v", fileName)
		swaggerJsn, conversionErr := utills.ToJSON(fileContent)
		if conversionErr != nil {
			loggers.LoggerAPI.Errorf("Error converting api file to json: %v", conversionErr.Error())
			return conversionErr
		}
		apiProject.OpenAPIJsn = swaggerJsn
		apiProject.APIType = constants.HTTP
	} else if strings.Contains(fileName, interceptorCertDir+string(os.PathSeparator)) &&
		(strings.HasSuffix(fileName, crtExtension) || strings.HasSuffix(fileName, pemExtension)) {
		if !tlsutils.IsPublicCertificate(fileContent) {
			loggers.LoggerAPI.Errorf("Provided interceptor certificate: %v is not in the PEM file format. ", fileName)
			return errors.New("interceptor certificate Validation Error")
		}
		apiProject.InterceptorCerts = append(apiProject.InterceptorCerts, fileContent...)
		apiProject.InterceptorCerts = append(apiProject.InterceptorCerts, newLineByteArray...)
	} else if strings.Contains(fileName, endpointCertDir+string(os.PathSeparator)) {
		if strings.Contains(fileName, endpointCertFile) {
			epCertJSON, conversionErr := utills.ToJSON(fileContent)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error converting %v file to json: %v", fileName, conversionErr.Error())
				return conversionErr
			}
			endpointCertificates := &EndpointCertificatesDetails{}
			err := json.Unmarshal(epCertJSON, endpointCertificates)
			if err != nil {
				loggers.LoggerAPI.Error("Error parsing content of endpoint certificates: ", err)
			} else if endpointCertificates != nil && len(endpointCertificates.Data) > 0 {
				for _, val := range endpointCertificates.Data {
					apiProject.EndpointCerts[val.Endpoint] = val.Certificate
				}
			}
		} else if strings.HasSuffix(fileName, crtExtension) || strings.HasSuffix(fileName, pemExtension) {
			if !tlsutils.IsPublicCertificate(fileContent) {
				loggers.LoggerAPI.Errorf("Provided certificate: %v is not in the PEM file format. ", fileName)
				// TODO: (VirajSalaka) Create standard error handling mechanism
				return errors.New("certificate Validation Error")
			}

			if fileNameArray := strings.Split(fileName, string(os.PathSeparator)); len(fileNameArray) > 0 {
				certFileName := fileNameArray[len(fileNameArray)-1]
				apiProject.UpstreamCerts[certFileName] = fileContent
			}
		}
	} else if (strings.Contains(fileName, apiYAMLFile) || strings.Contains(fileName, apiJSONFile)) &&
		!strings.Contains(fileName, openAPIDir) {
		loggers.LoggerAPI.Debugf("fileName : %v", fileName)
		apiJsn, conversionErr := utills.ToJSON(fileContent)
		if conversionErr != nil {
			loggers.LoggerAPI.Errorf("Error occurred converting api file to json: %v", conversionErr.Error())
			return conversionErr
		}
		var apiYaml APIYaml
		err = json.Unmarshal(apiJsn, &apiYaml)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error occurred while parsing api.yaml or api.json %v", err.Error())
			return err
		}
		apiYaml = PopulateEndpointsInfo(apiYaml)

		err = VerifyMandatoryFields(apiYaml)
		if err != nil {
			loggers.LoggerAPI.Errorf("%v", err)
			return err
		}
		apiProject.APIYaml = apiYaml
		ExtractAPIInformation(apiProject, apiYaml)
	} else if strings.Contains(fileName, policiesDir+string(os.PathSeparator)) { // handle policy dir
		if strings.HasSuffix(fileName, policySpecFileExtension) {
			// process policy specificationn
			spec := PolicySpecification{}
			if err := yaml.Unmarshal(fileContent, &spec); err != nil {
				loggers.LoggerAPI.Errorf("Error parsing content of policy specification %v: %v", fileName, err.Error())
				return err
			}

			key := utills.FileNameWithoutExtension(fileName)
			if policy, ok := apiProject.Policies[key]; ok {
				policy.Specification = spec
				apiProject.Policies[key] = policy
			} else {
				apiProject.Policies[key] = PolicyContainer{
					Specification: spec,
				}
			}

		}
		if strings.HasSuffix(fileName, policyDefFileExtension) {
			// process policy definition
			def := PolicyDefinition{RawData: fileContent}
			// if err := yaml.Unmarshal(fileContent, &def); err != nil {
			// 	loggers.LoggerAPI.Errorf("Error parsing content of policy definition %v: %v", fileName, err.Error())
			// 	return err
			// }

			key := utills.FileNameWithoutExtension(fileName)
			if policy, ok := apiProject.Policies[key]; ok {
				policy.Definition = def
				apiProject.Policies[key] = policy
			} else {
				apiProject.Policies[key] = PolicyContainer{
					Definition: def,
				}
			}
		}
	}
	return nil
}

func parseDeployments(data []byte) ([]Deployment, error) {
	// deployEnvsFromAPI represents deployments read from API Project
	deployEnvsFromAPI := &DeploymentEnvironments{}
	if err := yaml.Unmarshal(data, deployEnvsFromAPI); err != nil {
		loggers.LoggerAPI.Errorf("Error parsing content of deployment environments: %v", err.Error())
		return nil, err
	}

	deployments := make([]Deployment, 0, len(deployEnvsFromAPI.Data))
	for _, deployFromAPI := range deployEnvsFromAPI.Data {
		defaultVhost, exists, err := config.GetDefaultVhost(deployFromAPI.DeploymentEnvironment)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error reading default vhost of environment %v: %v",
				deployFromAPI.DeploymentEnvironment, err.Error())
			return nil, err
		}
		// if the environment is not configured, ignore it
		if !exists {
			continue
		}

		deployment := deployFromAPI
		// if vhost is not defined with the API project use the default vhost from config
		if deployFromAPI.DeploymentVhost == "" {
			deployment.DeploymentVhost = defaultVhost
		}
		deployments = append(deployments, deployment)
	}
	return deployments, nil
}

func parsePolicies() {

}
