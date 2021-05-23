# Integration Tests

### How to create APIs, Applications, and Subscriptions for testcases
Add APIs, Applications, and Subscriptions to the json files in `integration/test-integration/src/test/resources/apimApisAppsSubs`
These will be added to API Manager before starting any of the testcases

### How to Avoid API Manager restarting everytime the test are run

1. Run the tests ones
2. Copy the `apim` folder in the `integration/test-integration/target` to a different location
3. cd into that `apim` folder and run `docker-compose up`
4. Comment the test class `ApimStartupShutdownExecutor` in `integration/test-integration/src/test/resources/testng-cc-with-apim.xml`. 
   Then it should look like,
   ```
    <!--  <class name="org.wso2.choreo.connect.tests.setup.withapim.ApimStartupShutdownExecutor"/> -->
   ```
5. Now run the tests with `mvn clean install` while in the `integration/test-integration` folder
6. Once done, uncomment the previously commented `ApimStartupShutdownExecutor` test class, stop the running apim 
   instance, and run all the tests with `mvn clean install`.