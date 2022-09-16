name: "🐞 Report a Bug"
description: Create an issue if something does not work as expected.
labels: ["Priority/Normal", "Type/Bug"]
body:
  - type: textarea
    id: description
    attributes:
      label: Description
      description: Describe the issue.
    validations:
      required: true
  - type: textarea
    id: steps
    attributes:
      label: Steps to Reproduce
      description: List the steps you followed when you encountered the issue. If none, type N/A.
    validations:
      required: true
  - type: input
    id: version
    attributes:
      label: Version
      description: Enter component version.
    validations:
      required: true
  - type: textarea
    id: environment
    attributes:
      label: Environment Details (with versions)
      description: Mention the environment details (OS, Client, etc.) that the product is running on.
    validations:
      required: false
  - type: textarea
    id: logs
    attributes:
      label: Relevant Log Output
      description: Copy and paste any relevant log output.
      render: shell
    validations:
      required: false
  - type: textarea
    id: related
    attributes:
      label: Related Issues
      description: Mention if any related issues.
    validations:
      required: false
  - type: input
    id: suggested
    attributes:
      label: Suggested Labels
      description: Mention if any suggested labels.
    validations:
      required: false