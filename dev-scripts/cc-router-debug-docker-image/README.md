# Build Choreo Connect Route Profile (CPU/Memory Profiling) Docker Image

Default envoy docker image do not ship with gperftools. Following steps build envoy binary statistically links gperftools and create Choreo Connect Router docker image which can be used to profile memory and CPU.

## Variables

Variable for the script [build-cc-profile-docker-image.sh](build-cc-router-debug-docker-image.sh).

| Variable           | Description                                                        |
|--------------------|:-------------------------------------------------------------------|
| ENVOY_REPO         | Existing or new dir to clone envoy Git repo                        |
| ENVOY_TAG          | Envoy Git tag relevant to envoy version                            |
| CC_BASE_IMAGE      | Released Choreo Connect Router Docker image                        |
| CC_PROFILE_IMAGE   | New image name for the profiled Choreo Connect Router Docker image |
| ENVOY_DOCKER_IMAGE | Optionally build Envoy Docker image with built envoy binary        |

Following are some sample values.

```sh
export ENVOY_REPO=./envoy
export ENVOY_TAG=v1.24.1
export CC_BASE_IMAGE=wso2/choreo-connect-router:1.2.0
export CC_PROFILE_IMAGE="" # use default image name
export ENVOY_DOCKER_IMAGE="" # do not build an envoy docker image
```

## Execute the script

**Note:**
Make sure you do not have uncommitted changes in envoy git repository if you are referring an existing git repo. This script will not force create branch `"$ENVOY_TAG-branch"`. If it exists assuming that would be a user branch and exit with error.

-   Execute the script.

    ```sh
    ./build-cc-router-debug-docker-image.sh
    ```

-   Or run in background.

    ```sh
    nohup ./build-cc-router-debug-docker-image.sh > out.log 2>&1 &
    echo $! > save_pid.txt
    tail -f out.log
    ```

## Cleanup Space

Clean caches for incremental builds.

```sh
sudo rm -rf /tmp/envoy-docker-build/
rm -rf "$ENVOY_REPO"/linux
```
