#!/bin/bash

#Adapted from https://github.com/RedHatInsights/insights-ingress-go/blob/master/pr_check.sh

# --------------------------------------------
# Options that must be configured by app owner
# --------------------------------------------
APP_NAME="eventing"  # name of app-sre "application" folder this component lives in
COMPONENT_NAME="eventing-splunk-quarkus"  # name of app-sre "resourceTemplate" in deploy.yaml for this component
IMAGE="quay.io/cloudservices/eventing-integrations"
OLD_IMAGE="quay.io/cloudservices/eventing-integrations-splunk"
DOCKERFILE="splunk-quarkus/Dockerfile.jvm"

# Enviroment to take bonfire/clowdapp reources from
export REF_ENV="insights-stage"

IQE_PLUGINS="eventing"
IQE_MARKER_EXPRESSION="smoke" # Need to check this
IQE_FILTER_EXPRESSION=""
IQE_CJI_TIMEOUT="30m"


# Install bonfire repo/initialize
CICD_URL=https://raw.githubusercontent.com/RedHatInsights/bonfire/master/cicd
curl -s $CICD_URL/bootstrap.sh > .cicd_bootstrap.sh && source .cicd_bootstrap.sh
source $CICD_ROOT/build.sh

# Compatiblity before migration
set -x
podman tag "${IMAGE}:${IMAGE_TAG}" "${OLD_IMAGE}:${IMAGE_TAG}"
podman push "${OLD_IMAGE}:${IMAGE_TAG}"
export IMAGE=$OLD_IMAGE
set +x

export COMPONENTS_W_RESOURCES="policies-engine"
source $CICD_ROOT/deploy_ephemeral_env.sh

source $CICD_ROOT/cji_smoke_test.sh
