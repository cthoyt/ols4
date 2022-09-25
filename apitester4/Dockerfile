
FROM maven:3.8-jdk-11

ADD . /opt/apitester
RUN cd /opt/apitester && mvn clean package

ENTRYPOINT [ "java", "-jar", "/opt/apitester/target/apitester-1.0-SNAPSHOT.jar" ]


