FROM openjdk:15-jdk-oraclelinux8

ADD build/distributions/auth.tar ./
WORKDIR ./auth
EXPOSE 8080

ENTRYPOINT ["bin/webservice"]
