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

// SubscriptionDataSpec defines the desired state of SubscriptionData
type SubscriptionDataSpec struct {
	// INSERT ADDITIONAL SPEC FIELDS - desired state of cluster
	// Important: Run "make" to regenerate code after modifying this file

	Subscriptions []Subscription `json:"subscriptions"`
}

// Subscription defines the properties of a subscription
type Subscription struct {
	UUID               string `json:"uuid"`
	Name               string `json:"name"`
	APIDisplayName     string `json:"apiDisplayName"`
	BusinessPlan       string `json:"businessPlan"`
	SubscriptionStatus string `json:"subscriptionStatus"`
	LifecycleStatus    string `json:"lifecycleStatus"`
	AppRef             string `json:"appRef"`
}

// SubscriptionDataStatus defines the observed state of SubscriptionData
type SubscriptionDataStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "make" to regenerate code after modifying this file
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status

// SubscriptionData is the Schema for the subscriptiondata API
type SubscriptionData struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   SubscriptionDataSpec   `json:"spec,omitempty"`
	Status SubscriptionDataStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// SubscriptionDataList contains a list of SubscriptionData
type SubscriptionDataList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []SubscriptionData `json:"items"`
}

func init() {
	SchemeBuilder.Register(&SubscriptionData{}, &SubscriptionDataList{})
}
