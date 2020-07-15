#!/bin/bash

set -ex

OWN_PATH=`realpath $0`
FUNCTION_DIR=`realpath "$(dirname $OWN_PATH)/../amplify/backend/function/"`

GRAPHQL_RESOLVER_SRC_DIR="$FUNCTION_DIR/GraphQLResolver/src"
CLOUDFRONT_ORIGIN_REQUEST_SRC_DIR="$FUNCTION_DIR/CloudfrontOriginRequest/src"
POST_AUTHENTICATION_SRC_DIR="$FUNCTION_DIR/PostAuthentication/src"
DYNAMODB_STREAM_SRC_DIR="$FUNCTION_DIR/DynamoDBStream/src"
SYNC_ANNOTATION_COLLECTION_SRC_DIR="$FUNCTION_DIR/SyncAnnotationCollection/src"

# Install any required runtime deps.
(cd $GRAPHQL_RESOLVER_SRC_DIR && npm install --production)

(cd $CLOUDFRONT_ORIGIN_REQUEST_SRC_DIR && npm install --production)

(cd $POST_AUTHENTICATION_SRC_DIR && npm install --production)

(cd $DYNAMODB_STREAM_SRC_DIR && npm install --production)

(cd $SYNC_ANNOTATION_COLLECTION_SRC_DIR && npm install --production)
