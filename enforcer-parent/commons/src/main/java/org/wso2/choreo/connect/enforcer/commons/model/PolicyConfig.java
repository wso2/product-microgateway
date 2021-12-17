package org.wso2.choreo.connect.enforcer.commons.model;

import java.util.ArrayList;

/**
 * policy configurations
 */
public class PolicyConfig {

    private ArrayList<Policy> in;
    private ArrayList<Policy> out;
    private ArrayList<Policy> fault;

    public ArrayList<Policy> getIn() {
        return in;
    }

    public void setIn(ArrayList<Policy> in) {
        this.in = in;
    }

    public ArrayList<Policy> getOut() {
        return out;
    }

    public void setOut(ArrayList<Policy> out) {
        this.out = out;
    }

    public ArrayList<Policy> getFault() {
        return fault;
    }

    public void setFault(ArrayList<Policy> fault) {
        this.fault = fault;
    }
}
