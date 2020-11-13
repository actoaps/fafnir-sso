FROM openjdk:15-jdk-alpine

ADD build/distributions/auth.tar ./
WORKDIR ./auth
EXPOSE 8080

ENTRYPOINT ["bin/webservice"]
