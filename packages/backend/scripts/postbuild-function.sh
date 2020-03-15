#!/bin/bash

set -ex

OWN_PATH=`realpath $0`
FUNCTION_DIR=`realpath "$(dirname $OWN_PATH)/../amplify/backend/function/"`

GRAPHQL_RESOLVER_SRC_DIR="$FUNCTION_DIR/GraphQLResolver/src"

# Prune unneeded deps that are too large for lambda.
(cd $GRAPHQL_RESOLVER_SRC_DIR && \
  rm -rf node_modules && npm install --production)
