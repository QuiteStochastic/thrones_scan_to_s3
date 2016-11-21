FROM ubuntu:16.04
RUN mkdir /code
WORKDIR /code

RUN apt-get -y update && \
    apt-get -y upgrade && \
    apt-get -y install apt-utils

RUN apt-get -y update && \
    apt-get -y install openjdk-8-jre

ADD ./thrones_scan_to_s3/dev_user.txt /code
ADD ./thrones_scan_to_s3/target/thrones_scan_to_s3-1.0-SNAPSHOT.jar /code
ADD ./thrones_db_spring/src/main/resources/static /code/static


CMD java -jar ./thrones_scan_to_s3-1.0-SNAPSHOT.jar thrones_scan_to_s3.ScanToS3
