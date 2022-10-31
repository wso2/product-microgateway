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

package controllers

import (
	"context"

	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/subscription"
	"k8s.io/apimachinery/pkg/runtime"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/log"

	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	cpv1alpha1 "github.com/wso2/product-microgateway/adapter/internal/operator/api/v1alpha1"
)

// ApplicationDataReconciler reconciles a ApplicationData object
type ApplicationDataReconciler struct {
	client.Client
	Scheme *runtime.Scheme
}

//+kubebuilder:rbac:groups=cp.wso2.com,resources=applicationdata,verbs=get;list;watch;create;update;patch;delete
//+kubebuilder:rbac:groups=cp.wso2.com,resources=applicationdata/status,verbs=get;update;patch
//+kubebuilder:rbac:groups=cp.wso2.com,resources=applicationdata/finalizers,verbs=update

// Reconcile is part of the main kubernetes reconciliation loop which aims to
// move the current state of the cluster closer to the desired state.
// TODO(user): Modify the Reconcile function to compare the state specified by
// the ApplicationData object against the actual cluster state, and then
// perform operations to make the cluster state reflect the state specified by
// the user.
//
// For more details, check Reconcile and its Result here:
// - https://pkg.go.dev/sigs.k8s.io/controller-runtime@v0.13.0/pkg/reconcile
func (r *ApplicationDataReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	_ = log.FromContext(ctx)

	// TODO(user): your logic here
	log.Log.Info("Reconciling application data for... %s/%s\n", req.NamespacedName.Namespace, req.NamespacedName.Name)
	// TODO: need logic to pick a single resource
	// use the oldest one if creation time is same then use name alphabetical order of the
	// name as the tie breaker

	var applicationDataList cpv1alpha1.ApplicationDataList
	if err := r.List(ctx, &applicationDataList); err != nil {
		return ctrl.Result{}, err
	}

	// create application resource list from loaded apps
	var applications []cpv1alpha1.Application
	if len(applicationDataList.Items) > 0 {
		applications = applicationDataList.Items[0].Spec.Applications
	}

	appList := marshalApplicationList(applications)
	xds.UpdateEnforcerApplications(appList)

	subList := marshalSubscriptionList(applications)
	xds.UpdateEnforcerSubscriptions(subList)

	appKeyMappingList := marshalApplicationKeyMapping(applications)
	xds.UpdateEnforcerApplicationKeyMappings(appKeyMappingList)

	return ctrl.Result{}, nil
}

func marshalApplicationList(applicationList []cpv1alpha1.Application) *subscription.ApplicationList {
	applications := []*subscription.Application{}
	for _, appInternal := range applicationList {
		app := &subscription.Application{
			Uuid:       appInternal.UUID,
			Name:       appInternal.Name,
			Policy:     appInternal.Policy,
			Attributes: appInternal.Attributes,
		}
		applications = append(applications, app)
	}
	return &subscription.ApplicationList{
		List: applications,
	}
}

func marshalSubscriptionList(applicationList []cpv1alpha1.Application) *subscription.SubscriptionList {
	subscriptions := []*subscription.Subscription{}
	for _, appInternal := range applicationList {
		for _, subInternal := range appInternal.Subscriptions {
			sub := &subscription.Subscription{
				SubscriptionUUID:  subInternal.UUID,
				PolicyId:          subInternal.PolicyID,
				SubscriptionState: subInternal.SubscriptionStatus,
				AppUUID:           appInternal.UUID,
				// ApiUUID: subInternal.ApiRef,
			}
			subscriptions = append(subscriptions, sub)
		}
	}
	return &subscription.SubscriptionList{
		List: subscriptions,
	}
}

func marshalApplicationKeyMapping(applicationList []cpv1alpha1.Application) *subscription.ApplicationKeyMappingList {
	applicationKeyMappings := []*subscription.ApplicationKeyMapping{}
	for _, appInternal := range applicationList {
		for _, consumerKeyInternal := range appInternal.ConsumerKeys {
			consumerKey := &subscription.ApplicationKeyMapping{
				ConsumerKey:     consumerKeyInternal.Key,
				KeyManager:      consumerKeyInternal.KeyManager,
				ApplicationUUID: appInternal.UUID,
			}
			applicationKeyMappings = append(applicationKeyMappings, consumerKey)
		}
	}
	return &subscription.ApplicationKeyMappingList{
		List: applicationKeyMappings,
	}
}

// SetupWithManager sets up the controller with the Manager.
func (r *ApplicationDataReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(mgr).
		For(&cpv1alpha1.ApplicationData{}).
		Complete(r)
}
