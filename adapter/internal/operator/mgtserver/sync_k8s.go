package mgtserver

import (
	"context"

	cpv1alpha1 "github.com/wso2/product-microgateway/adapter/internal/operator/api/v1alpha1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// StartSync creates ApplicationData custom resource
func StartSync(c client.Client) {

	// TODO: Listener for mgt server responses

	// Using a typed object.
	pod := &cpv1alpha1.ApplicationData{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "applicationdata-sample-creted-by-go-client",
		},
		Spec: cpv1alpha1.ApplicationDataSpec{
			Applications: []cpv1alpha1.Application{
				{
					UUID:   "8s7gf8a7sda97d9as8d",
					Name:   "appzz",
					Owner:  "Alice",
					Policy: "10PerMin",
					ConsumerKeys: []cpv1alpha1.ConsumerKey{
						{
							Key:        "hef14gh8syDvTtvoWYeIHYbjF_Ya",
							KeyManager: "Resident Key Manager",
						},
					},
				},
			},
		},
	}
	// c is a created client.
	_ = c.Create(context.Background(), pod)
}
