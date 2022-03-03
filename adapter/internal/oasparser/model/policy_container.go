/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package model

import (
	"bytes"
	"errors"
	"fmt"
	"text/template"

	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
	"gopkg.in/yaml.v2"
)

const (
	policyCCGateway string = "ChoreoConnect"
)

// policy value validation types
const (
	policyValTypeString string = "String"
	policyValTypeInt    string = "Integer"
	policyValTypeBool   string = "Boolean" // TODO: (renuka) check type names with APIM
	policyValTypeArray  string = "Array"
	policyValTypeMap    string = "Map"
)

// PolicyFlow holds list of Policies in a operation (in one flow: In, Out or Fault)
type PolicyFlow string

const (
	policyInFlow    PolicyFlow = "request"
	policyOutFlow   PolicyFlow = "response"
	policyFaultFlow PolicyFlow = "fault"
)

// PolicyContainerMap maps PolicyName -> PolicyContainer
type PolicyContainerMap map[string]PolicyContainer

// PolicyContainer holds the definition and specification of policy
type PolicyContainer struct {
	Specification PolicySpecification
	Definition    PolicyDefinition
}

// PolicySpecification holds policy specification from ./Policy/<policy>.yaml files
type PolicySpecification struct {
	Type    string `yaml:"type" json:"type"`
	Version string `yaml:"version" json:"version"`
	Data    struct {
		Name              string   `yaml:"name"`
		ApplicableFlows   []string `yaml:"applicableFlows"`
		SupportedGateways []string `yaml:"supportedGateways"`
		SupportedAPITypes []string `yaml:"supportedApiTypes"`
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
	Definition struct {
		Action     string                 `yaml:"action"`
		Parameters map[string]interface{} `yaml:"parameters"`
	} `yaml:"definition"`
	RawData []byte `yaml:"-"`
}

// GetFormattedOperationalPolicies returns formatted, Choreo Connect policy from a user templated policy
// here, the struct swagger is only used for logging purpose, in case if we introduce logger context to get org ID, API ID, we can remove it from here
func (p PolicyContainerMap) GetFormattedOperationalPolicies(policies OperationPolicies, swagger *MgwSwagger) OperationPolicies {
	fmtPolicies := OperationPolicies{}

	inFlowStats := policies.Request.getStats()
	for i, policy := range policies.Request {
		if fmtPolicy, err := p.getFormattedPolicyFromTemplated(policy, policyInFlow, inFlowStats, i, swagger); err == nil {
			fmtPolicies.Request = append(fmtPolicies.Request, fmtPolicy)
		}
	}

	outFlowStats := policies.Response.getStats()
	for i, policy := range policies.Response {
		if fmtPolicy, err := p.getFormattedPolicyFromTemplated(policy, policyOutFlow, outFlowStats, i, swagger); err == nil {
			fmtPolicies.Response = append(fmtPolicies.Response, fmtPolicy)
		}
	}

	faultFlowStats := policies.Fault.getStats()
	for i, policy := range policies.Fault {
		if fmtPolicy, err := p.getFormattedPolicyFromTemplated(policy, policyFaultFlow, faultFlowStats, i, swagger); err == nil {
			fmtPolicies.Fault = append(fmtPolicies.Fault, fmtPolicy)
		}
	}

	return fmtPolicies
}

// getFormattedPolicyFromTemplated returns formatted, Choreo Connect policy from a user templated policy
func (p PolicyContainerMap) getFormattedPolicyFromTemplated(policy Policy, flow PolicyFlow, stats map[string]policyStats, index int, swagger *MgwSwagger) (Policy, error) {
	// using index i instead of struct to validate policy against multiple allowed and apply only first if multiple exists
	spec := p[policy.PolicyName].Specification
	if err := spec.validatePolicy(policy, flow, stats, index); err != nil {
		swagger.GetID()
		loggers.LoggerOasparser.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Operation policy validation failed for API \"%s\" in org \"%s\":, ignoring the policy \"%s\": %v", swagger.GetID(), swagger.OrganizationID, policy.PolicyName, err),
			Severity:  logging.MINOR,
			ErrorCode: 2204,
		})
		return policy, err
	}

	defRaw := p[policy.PolicyName].Definition.RawData
	t, err := template.New("policy-def").Parse(string(defRaw))
	if err != nil {
		loggers.LoggerOasparser.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error parsing the operation policy definition \"%s\" into go template of the API \"%s\" in org \"%s\": %v", policy.PolicyName, swagger.GetID(), swagger.OrganizationID, err),
			Severity:  logging.MINOR,
			ErrorCode: 2205,
		})
		return Policy{}, err
	}

	var out bytes.Buffer
	err = t.Execute(&out, policy.Parameters)
	if err != nil {
		loggers.LoggerOasparser.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error parsing operation policy definition \"%s\" of the API \"%s\" in org \"%s\": %v", policy.PolicyName, swagger.GetID(), swagger.OrganizationID, err),
			Severity:  logging.MINOR,
			ErrorCode: 2206,
		})
		return Policy{}, err
	}

	def := PolicyDefinition{}
	if err := yaml.Unmarshal(out.Bytes(), &def); err != nil {
		loggers.LoggerOasparser.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error parsing formalized operation policy definition \"%s\" into yaml of the API \"%s\" in org \"%s\": %v", policy.PolicyName, swagger.GetID(), swagger.OrganizationID, err),
			Severity:  logging.MINOR,
			ErrorCode: 2207,
		})
		return Policy{}, err
	}

	// update templated policy itself and return, not updating a pointer to keep the original template values as it is.
	policy.Parameters = def.Definition.Parameters
	policy.Action = def.Definition.Action

	return policy, nil
}

// validatePolicy validates the given policy against the spec
func (spec *PolicySpecification) validatePolicy(policy Policy, flow PolicyFlow, stats map[string]policyStats, index int) error {
	if spec.Data.Name != policy.PolicyName {
		return fmt.Errorf("invalid policy specification, spec name \"%s\" and policy name \"%s\" mismatch", spec.Data.Name, policy.PolicyName)
	}
	if !arrayContains(spec.Data.ApplicableFlows, string(flow)) {
		return fmt.Errorf("policy flow \"%s\" not supported", flow)
	}
	if !arrayContains(spec.Data.SupportedGateways, policyCCGateway) {
		return errors.New("choreo connect gateway not supported")
	}
	if !spec.Data.MultipleAllowed { // TODO (renuka): remove this multiple allowed validation and compute stats
		// TODO (renuka): check the behaviour with APIM
		// in here allow first instance of policy to be applied if multiple is found
		pStat := stats[policy.PolicyName]
		if pStat.count > 1 {
			if index != pStat.firstIndex {
				return errors.New("multiple policies not allowed")
			}
			loggers.LoggerOasparser.Warnf("Operation policy \"%s\" not allowed in multiple times, appling the first policy", policy.PolicyName)
		}
	}

	policyPrams, ok := policy.Parameters.(map[string]interface{})
	if ok {
		for _, attrib := range spec.Data.PolicyAttributes {
			if _, found := policyPrams[attrib.Name]; attrib.Required && !found {
				return fmt.Errorf("required paramater \"%s\" not found", attrib.Name)
			}
		}
	}

	return nil
}
