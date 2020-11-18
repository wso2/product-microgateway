package org.wso2am.micro.gw.tests.dummy;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.wso2am.micro.gw.tests.IntegrationTest;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
@Category(IntegrationTest.class)
public class DummyTestCase {

    @ClassRule
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("enforcer",9095);



    @Before
    public void setData() {
        //environment.start();


    }

    @Test
    public void testAssertEqualsFalse() {
        //  processed the item
        int one_ = 1;
        int two_ = 2;

        assertEquals(true,true);
    }
}
