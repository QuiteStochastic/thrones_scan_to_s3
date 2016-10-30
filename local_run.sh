#!/usr/bin/env bash

#git pull

##build the jar
mvn clean package


##build the docker image
docker build -t thrones_scan_to_s3 .


##this is just the exit code of previous command
exit_code=$?


if [[ ${exit_code} == 0 ]]
then
    #echo "running thrones server"
    docker run thrones_scan_to_s3
fi