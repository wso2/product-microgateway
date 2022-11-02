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
	"reflect"

	"k8s.io/apimachinery/pkg/runtime"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/log"

	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	cpv1alpha1 "github.com/wso2/product-microgateway/adapter/internal/operator/api/v1alpha1"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/subscription"
)

// ApplicationReconciler reconciles a Application object
type ApplicationReconciler struct {
	client.Client
	Scheme    *runtime.Scheme
	appStatus *ApplicationsStatus
}

type ApplicationsStatus map[string]int64

//+kubebuilder:rbac:groups=cp.wso2.com,resources=applications,verbs=get;list;watch;create;update;patch;delete
//+kubebuilder:rbac:groups=cp.wso2.com,resources=applications/status,verbs=get;update;patch
//+kubebuilder:rbac:groups=cp.wso2.com,resources=applications/finalizers,verbs=update

// Reconcile is part of the main kubernetes reconciliation loop which aims to
// move the current state of the cluster closer to the desired state.
// TODO(user): Modify the Reconcile function to compare the state specified by
// the Application object against the actual cluster state, and then
// perform operations to make the cluster state reflect the state specified by
// the user.
//
// For more details, check Reconcile and its Result here:
// - https://pkg.go.dev/sigs.k8s.io/controller-runtime@v0.13.0/pkg/reconcile
func (r *ApplicationReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	_ = log.FromContext(ctx)

	logger.LoggerOperator.Info("Reconciling application: " + req.NamespacedName.String())

	var applicationList cpv1alpha1.ApplicationList
	if err := r.List(ctx, &applicationList); err != nil {
		return ctrl.Result{}, err
	}

	// create application resource list from loaded apps
	var applications []cpv1alpha1.ApplicationSpec
	if len(applicationList.Items) > 0 {
		for _, application := range applicationList.Items {
			applications = append(applications, application.Spec)
		}
	}

	newAppStatus := generateApplicationStaus(applicationList)

	if shouldSendUpdates(r.appStatus, newAppStatus) {
		appList := marshalApplicationList(applications)
		xds.UpdateEnforcerApplications(appList)

		subList := marshalSubscriptionList(applications)
		xds.UpdateEnforcerSubscriptions(subList)

		appKeyMappingList := marshalApplicationKeyMapping(applications)
		xds.UpdateEnforcerApplicationKeyMappings(appKeyMappingList)
	}

	r.appStatus = newAppStatus

	return ctrl.Result{}, nil
}

func shouldSendUpdates(currentAppStatus *ApplicationsStatus, newAppStatus *ApplicationsStatus) bool {
	return !reflect.DeepEqual(currentAppStatus, newAppStatus)
}

func generateApplicationStaus(applicationList cpv1alpha1.ApplicationList) *ApplicationsStatus {
	var appStatus = make(ApplicationsStatus)
	for _, application := range applicationList.Items {
		namespacedName := application.Namespace + "/" + application.Name
		appStatus[namespacedName] = application.Generation
	}
	return &appStatus
}

func marshalApplicationList(applicationList []cpv1alpha1.ApplicationSpec) *subscription.ApplicationList {
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

func marshalSubscriptionList(applicationList []cpv1alpha1.ApplicationSpec) *subscription.SubscriptionList {
	subscriptions := []*subscription.Subscription{}
	for _, appInternal := range applicationList {
		for _, subInternal := range appInternal.Subscriptions {
			sub := &subscription.Subscription{
				SubscriptionUUID:  subInternal.UUID,
				PolicyId:          subInternal.PolicyID,
				SubscriptionState: subInternal.SubscriptionStatus,
				AppUUID:           appInternal.UUID,
			}
			subscriptions = append(subscriptions, sub)
		}
	}
	return &subscription.SubscriptionList{
		List: subscriptions,
	}
}

func marshalApplicationKeyMapping(applicationList []cpv1alpha1.ApplicationSpec) *subscription.ApplicationKeyMappingList {
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
func (r *ApplicationReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(mgr).
		For(&cpv1alpha1.Application{}).
		Complete(r)
}
