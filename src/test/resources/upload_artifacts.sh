#!/bin/sh

ARTIFACT=$1
REPOSITORY=$2
ITERATIONS=$3

if [ -z "$ARTIFACT" ]; then
    echo "No artifact path specified exiting..."
    exit 1
fi

if [ -z "$ITERATIONS" ]; then
    ITERATIONS=5
fi

for i in `seq 1 $ITERATIONS`;
do
    echo "Creating artifact $i"
    curl -v -u admin:admin123 "http://localhost:8081/service/rest/v1/components?repository=@$REPOSITORY" -H "accept: application/json" -H "Content-Type: multipart/form-data" -F "maven2.groupId=com.blackducksoftware.test$i" -F "maven2.artifactId=test" -F "maven2.version=$i.0.0" -F "maven2.asset1=@$ARTIFACT" -F "maven2.asset1.extension=jar"
done