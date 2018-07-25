FROM openjdk:9.0.1-11-slim

ADD build/distributions/auth.tar ./
WORKDIR ./auth
EXPOSE 8080

ENTRYPOINT ["bin/auth"]
