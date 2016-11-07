FROM ubuntu:16.04
RUN mkdir /code
WORKDIR /code

RUN apt-get -y update && \
    apt-get -y upgrade && \
    apt-get -y install apt-utils

RUN apt-get -y update && \
    apt-get -y install openjdk-8-jre

ADD ./dev_user.txt /code
ADD ./target/thrones_scan_to_s3-1.0-SNAPSHOT.jar /code


CMD java -jar ./thrones_scan_to_s3-1.0-SNAPSHOT.jar thrones_scan_to_s3.ScanToS3
