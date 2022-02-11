# Integration Tests

## Standalone Mode (CC in Standalone Mode)
### How to Add a Testcase 
1. Add the testcase class to an existing or a new folder in 
   `src/test/java/org/wso2/choreo/connect/tests/testcases/standalone`
2. If an api needs to be deployed,     
   i. add its openapi/swagger definition to `src/test/resources/openAPIs`       
   ii. create and deploy the project in the class `src/test/java/org/wso2/choreo/connect/tests/setup/standalone/CcWithDefaultConf.java`
      - reason to create and deploy all apis in one location is to reduce the waiting time to deploy
      - `CcWithDefaultConf.java` is the Choreo Connect with the default config toml. Other such classes in the 
        `setup/standalone` folder represents different Choreo Connect instances with different configurations. 
        You can pick accordingly.
3. Within the testcase, assume that the api has already been deployed and invoke as needed.
4. Test classes does not have a base test class in the standalone mode. Therefore, the test class does not need to extend 
   a parent class.
5. Importantly, add the testcase to the `src/test/resources/testng-cc-standalone.xml` file. Pick the group
   according to the class you deployed your API in 2. ii. 

## WithAPIM Mode (CC with API-M as Control Plane)
In this mode, we use a class named `ApimPreparer` to send all the necessary artifacts to API Manager.
Currently, we do create APIs manually as well. But `ApimPreparer`,
   1. allows directly using the REST API JSON payload visible in the browser to create an API
   2. eliminates the waiting time between deployments in each testcase (we deploy everything together and then wait)
   3. lets us identify any transient errors that could occur when deploying in bulk

In the testng file for this mode (`integration/test-integration/src/test/resources/testng-cc-with-apim.xml`), 
we call the `ApimPreparer` class whenever we want to deploy the artifacts. When we do, we also mention the index 
of the artifacts set. Example:

```xml
<test name="Clean and deploy APIs apps subscriptions in set 1" parallel="false">
    <parameter name = "apimArtifactsIndex" value="1"/>
    <classes>
        <class name="org.wso2.choreo.connect.tests.setup.withapim.ApimPreparer"/>
    </classes>
</test>
```

This deploys everything that is inside the folder `integration/test-integration/src/test/resources/apim/1` to API Manager,
which then gets sent to Choreo Connect, because all the CC instances run in the `testng-cc-with-apim.xml` file are 
configured to run with API-M. 

### How to Add a Testcase

#### How to write the testcase
> Start by first creating the testcase class as given here, and then move to the next section which describes how to create
> the API you need for your testcase.
1. Add a testcase class to `src/test/java/org/wso2/choreo/connect/tests/testcases/withapim`
2. Extend the class `org.wso2.choreo.connect.tests.apim.ApimBaseTest` and use `super.initWithSuperTenant()` **or**
   `super.init(userMode)` as required.
3. Assume that API-M and CC have already started and invoke APIs using the util methods.
      NOTE: Currently, `ApimPreparer` is only capable of deploying APIs, Applications, and Subscriptions. Therefore, changes
      on admin side needs to be deployed within the testcase and cleaned within the testcase as well.
4. Add your testcase to `testng-cc-with-apim.xml` depending on the CC instance this must run with (Some instances may
have been specially configured to suit certain test cases). If the default config is enough, just add your testcase 
to one of the top groups. When in doubt, always refer to the `name` in the `<test >` tag.

#### How to create the APIs, Apps, Subscriptions needed by your testcase
1. Have a look at `testng-cc-with-apim.xml` and figure out when to deploy your API depending on where your testcase 
is added. Check `apimArtifactsIndex` value for the `ApimPreparer` that is right above your testcase.
2. Then update the files in the following locations.
   1. APIs - `integration/test-integration/src/test/resources/apim/<apimArtifactsIndex>/apis`
      1. Here each file contains a minimal REST API JSON payload sent to API-M Publisher
         1. Note: Make sure the file name is the name of the API. Example: If the API name is `BlockedApi` then 
         the file name is `BlockedApi.json`.
      2. Avoid adding the fields `provider` and `tags` because they automatically get added in a way supported by the test utils.
      3. To include an OpenAPI definition together with the API request payload,
         1. Add the OpenAPI definition to `integration/test-integration/src/test/resources/openAPIs`.
         2. Add a mapping entry to `integration/test-integration/src/test/resources/apim/<apimArtifactsIndex>/apiToOpenAPI.json`
         as given below.
            1. Format -> `<api-name>: <openapi-file-name>` (in other words, `<api-request-file-name>: <openapi-file-name>`)
            2. Example -> `"JwtScopeAPI": "scopes_openAPI.yaml"`
   2. Applications - `integration/test-integration/src/test/resources/apim/<apimArtifactsIndex>/app`
      1. Currently, only the App name and throttleTier are supported
   3. Subscriptions - `integration/test-integration/src/test/resources/apim/<apimArtifactsIndex>/subscriptions`
   4. Adding to vHosts - `integration/test-integration/src/test/resources/apim/1/apiToVhosts.json`
      1. This is not required for a regular testcase. Updating this file is only needed when,
         1. If the API must be deployed to more than one vHost
         2. If the API must be deployed to a vHost that is not localhost.
      2. An entry looks like,
         1. Format -> `<api-name>: <list-of-vhosts>`
         2. Example -> `"OneImportantAPI": ["a.wso2.com", "b.wso2.com"]`
   
#### Info about specific test group in the `testng-cc-with-apim.xml` file
- `Test APIs apps subscriptions pulled at Choreo Connect startup`: Here, we create the APIs, Applications, and 
  Subscriptions first and start CC afterwords. Thus, CC pulls them during startup, rather than pulling it after getting a 
  notification from eventhub. Since only the 1st method of the class actually tests whether the startup pull was
  successful, we only run the 1st method of the test class. Ex:
  ```
  <class name="org.wso2.choreo.connect.tests.testcases.withapim.BlockedApiTestCase">
      <methods><include name="testPublishedStateAPI"/></methods>
  </class>
  ```
- `Test APIs apps subscriptions fetched after eventhub events`: In this group, we test the eventhub scenario.
  By the time this group starts running, CC has already started.
  Therefore, the `ApimPreparer` first deletes the APIs, Applications, and Subscriptions that were already created.
  Then creates the same resources again, before running the tests.

### How to Avoid API Manager Restarting Everytime the Tests are Run
1. Run the tests ones
2. Copy the `apim` folder in the `integration/test-integration/target` to a different location
3. cd into that `apim` folder and run `docker-compose up`
4. Search and comment the three `Apim...Executer` the test classes in `integration/test-integration/src/test/resources/testng-cc-with-apim.xml`. 
   The three classes are `ApimStartupExecutor`, `ApimRestartExecutor` and `ApimShutdownExecutor`.
5. Now run the tests with `mvn clean install` while in the `integration/test-integration` folder
6. Once done, uncomment the previously commented `Apim...Executor` classes, stop the running apim 
   instance, and run all the tests with `mvn clean install`.
   
## Standalone Mode and Withapim Mode - How to Test with a new CC Instance
NOTE: Only if an instance with a completely new config is extremely necessary
1. Add the new config toml to `src/test/resources/configs` and rename it to resemble the requirement
2. Create a `setup` class
    i. for standalone mode, create a new class in `src/test/java/org/wso2/choreo/connect/tests/setup/standalone`
   with a name starting with `Cc`. This will start CC and also stop CC
    ii. for withapim mode, create a new class in `src/test/java/org/wso2/choreo/connect/tests/setup/withapim` 
   with a name starting with `CcStartupExecutor`. This will start CC. The existing class `CcShutdownExecutor` can be 
   commonly used to stop any `CcStartupExecutor`
3. Refer to the existing `CcStartupExecutor` and start the CC instance by specifying the config names added during step
   `1.`
4. in the testng file,
   i. for standalone mode, add a new test tag group, and add the new `Cc` class as the first in the group
   ii. for withapim mode, create a test group, and follow the pattern given below
   ```
   <test>
        <class name="ApimPreparer"/>
        <class name="CcStartupExecutor"/>
        .
        . . test classes to run . .
        .
        <class name="CcShutdownExecutor"/>
   </test>
   ```

## Withapim Mode - How to Test with a new API-M instance
NOTE: Not encouraged at all
1. Create a class similar to `org.wso2.choreo.connect.tests.setup.withapim.ApimStartupExecutor` and add a
   `beforeSuite` methods that starts the apim instance. (Don't have to write a new "shutdownExecuter",
   the existing can be used as it is. Yet, will have to make the `ApimInstance` class configurable like `CcInstance`)
2. Add a new testng file and follow the pattern given below
    ```
   <test>
        <class name="ApimStartupShutdownExecutorWithSpecialDepYaml"/>
        <class name="ApimClientsPreparer"/>
   </test>
   <test>
   </test>
   <test>
        <class name="ApimShutdownExecutor"/>
   </test>
   ```