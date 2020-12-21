package svcdiscovery

import (
	"fmt"
	"github.com/hashicorp/consul/api"
	"testing"
	"time"
)

func TestConsulWatcher_Watch(t *testing.T) {
	conf := api.DefaultConfig()
	conf.Address = "172.17.0.1"
	cl, _ := api.NewClient(conf)
	he := cl.Health()
	client := NewConsulClient(he)
	qo := api.QueryOptions{}

	//consul:[dc1,dc2].namespace.serviceA.[tag1,tag2];http://abc.com:80
	str := "consul:[local-dc,dc1].web.[*]"
	query, _ := ParseQueryString(str)
	//fmt.Println(query)
	q := Query{
		QString:      query,
		QueryOptions: &qo,
	}
	consulWatcher := NewConsulWatcher(client)

	doneChan := make(chan bool)
	nodeInfoChan := consulWatcher.Watch(q, doneChan)
	go func() {
		select {
		case <-time.After(5 * time.Second):
			doneChan <- true
		}

	}()
	for {
		select {
		case n, ok := <-nodeInfoChan:
			if !ok {
				return
			}
			fmt.Println("nodeInfo chan:", n)
		}

	}

}
