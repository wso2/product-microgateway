package org.wso2am.micro.gw.tests.dummy;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class DummyTestCase {
    @Test(description = "dummy test case")
    void alwaysTrue() {
        assertTrue(true);
    }
}
