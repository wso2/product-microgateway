package org.wso2am.mocro.gw.mockbackend;

public class Main {

    public static void main(String[] args) {
        int backEndServerPort = 2380;
        MockBackEndServer mockBackEndServer = new MockBackEndServer(backEndServerPort);
        mockBackEndServer.start();

    }
}
