#!/bin/sh
HOSTNAME=`hostname`
API_NAME=keycloak-user-storage

exec java -jar ${JAVA_OPTS} -Dspring.profiles.active=${PROFILE} `ls app/${API_NAME}*-SNAPSHOT.jar |tail -n1`