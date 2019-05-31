# Contributing to Microgateway

Hello There! Thank you very much for taking some time for contributing to [Microgateway](https://github.com/wso2/product-microgateway).

This will provide you the required guidelines for contributing to [Microgateway](https://github.com/wso2/product-microgateway)

## Table of Contents
[How To Contribute](#how-to-contribute)
  - [Reporting an issue](#reporting-an-issue)
  - [Your first contribution](#your-first-contribution)
  - [Contributing with code](#contributing-with-code)
    - [Understanding project structure](#understanding-project-structure)
    - [Development environment](#development-environment)
    - [Implementing solution](#implementing-solution)
    - [Debugging the toolkit](#debugging-the-toolkit)

[Ground Rules](#ground-rules)

## How to Contribute
### Reporting an issue
If you find an issue with the product, first search through the existing [issues](https://github.com/wso2/product-microgateway/issues) to find if the issue is already reported.
If your issue is not already reported, fill the project [issue template](https://github.com/wso2/product-microgateway/issues/new) and submit a new issue.
Be kind enough to provide all the details you can provide, when submitting your issue.

### Your first contribution
Not sure where to find a good issue to work on for the first time?
We mark issues with `good first issue` label so that starters can easily find their way in to contributing.
Visit [`good first issue`](https://github.com/wso2/product-microgateway/labels/good%20first%20issue) label, read through the issues and select one for you to work on.
After selecting the issue, communicate that to others by adding a comment. Before starting to work on the issue, discuss your plan/approach with project maintainers.
Then follow [Contributing with code](contributing-with-code) section.

### Contributing with code
If you are familiar with the project and its technologies, you no longer need to check `good first issue`. Browse through all open issues and pick one to work on. Check issues in [`help wanted`](https://github.com/wso2/product-microgateway/labels/help%20wanted) label, where we prioritize issues we need help from you.

#### Understanding project structure
We've two diffent components,
- Toolkit - Handles creating projects, importing APIs, building the project, etc...
- Runtime - Run the gateway by taking toolkit build artifact as input

Toolkit is implemented with java and Runtime is implemented with [ballerina-lang](https://ballerina.io).
Code for toolkit can be found in [micro-gateway-cli](https://github.com/wso2/product-microgateway/tree/master/components/micro-gateway-cli) component.
Code for runtime is in [micro-gateway-core](https://github.com/wso2/product-microgateway/tree/master/components/micro-gateway-core) component.
Also we've a small utility component based on Go-lang located at [micro-gateway-tools](https://github.com/wso2/product-microgateway/tree/master/components/micro-gateway-tools)

#### Development environment
1. JDK 8
1. Maven 3(Tested on 3.6.0)
1. Go
1. IntelliJ IDEA ot VSCode (Ballerina plugins currently support these two IDEs)

#### Implementing solution
Pick the correct component from above. Go through the compoenent code and find where you should add your solution. Implement it and use maven command `mvn clean install` to build the product.

#### Debugging the toolkit
1. Add remote debug configuration to your IDE.
1. Run required micro-gw command with `--java.debug <debug_port>` parameter. All tooklit commands accept this dev parameter allowing developers to debug java code.

Ex: `micro-gw init --java.debug 5005`

## Ground Rules
- Follow the contribution guidelines.
- Run the build with tests before submitting your PR.
- Be polite and respectful when communicating with others.
