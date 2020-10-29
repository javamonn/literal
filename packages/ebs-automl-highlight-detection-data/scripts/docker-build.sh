#!/bin/bash 

IMAGE_NAME=automl-highlight-detection-data
AWS_REGION=us-east-1
AWS_ACCOUNT_ID=046525304497
REPOSITORY_TAG="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${IMAGE_NAME}:latest"

docker build -t $IMAGE_NAME .
docker tag $IMAGE_NAME $REPOSITORY_TAG