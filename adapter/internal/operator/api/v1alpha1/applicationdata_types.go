/*
Copyright 2022.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

// ApplicationDataSpec defines the desired state of ApplicationData
type ApplicationDataSpec struct {
	// INSERT ADDITIONAL SPEC FIELDS - desired state of cluster
	// Important: Run "make" to regenerate code after modifying this file

	Applications []Application `json:"applications"`
}

// Application defines the properties of Application
type Application struct {
	UUID         string        `json:"uuid"`
	Name         string        `json:"name"`
	Owner        string        `json:"owner"`
	Policy       string        `json:"policy"`
	ConsumerKeys []ConsumerKey `json:"consumerKeys,omitempty"`
}

// ConsumerKey defines the consumer keys of Application
type ConsumerKey struct {
	Key        string `json:"key"`
	Type       string `json:"type"`
	KeyManager string `json:"keyManager"`
}

// ApplicationDataStatus defines the observed state of ApplicationData
type ApplicationDataStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "make" to regenerate code after modifying this file
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status

// ApplicationData is the Schema for the applicationdata API
type ApplicationData struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   ApplicationDataSpec   `json:"spec,omitempty"`
	Status ApplicationDataStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// ApplicationDataList contains a list of ApplicationData
type ApplicationDataList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []ApplicationData `json:"items"`
}

func init() {
	SchemeBuilder.Register(&ApplicationData{}, &ApplicationDataList{})
}
