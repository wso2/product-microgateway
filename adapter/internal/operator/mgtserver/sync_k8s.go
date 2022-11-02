package mgtserver

import (
	"context"

	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	cpv1alpha1 "github.com/wso2/product-microgateway/adapter/internal/operator/api/v1alpha1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// StartSync creates ApplicationData custom resource
func StartSync(c client.Client) {

	// TODO: Listener for mgt server responses

	// Using a typed object.
	application := &cpv1alpha1.Application{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "application-creted-by-go-client",
		},
		Spec: cpv1alpha1.ApplicationSpec{
			UUID:   "9ec2b927-47aa-456f-b6de-1959274f3asdasdz",
			Name:   "App5",
			Owner:  "Naskur",
			Policy: "10PerMin",
			Attributes: map[string]string{
				"att1": "val-1",
				"att2": "val-2",
			},
			ConsumerKeys: []cpv1alpha1.ConsumerKey{
				{Key: "yef14gh8syDvTt56rdtIHYbjF_Ya",
					KeyManager: "Resident Key Manager"},
			},
			Subscriptions: []cpv1alpha1.Subscription{{
				UUID:               "ff041d1b-be19-4529-a861-86a79905a1ad",
				Name:               "sub2",
				APIRef:             "PizzaShack",
				PolicyID:           "Unlimited",
				SubscriptionStatus: "ACTIVE",
				Subscriber:         "Bob",
			}},
		},
	}
	err := c.Create(context.Background(), application)
	if err != nil {
		logger.LoggerOperator.Info("Error creating resource: ", err)
	}
}
