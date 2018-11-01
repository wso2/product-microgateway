package org.wso2.apimgt.gateway.cli.model.config;

public class Etcd {
    public boolean isEtcdEnabled() {
        return etcdEnabled;
    }

    public void setEtcdEnabled(boolean etcdEnabled) {
        this.etcdEnabled = etcdEnabled;
    }

    private boolean etcdEnabled;
}
