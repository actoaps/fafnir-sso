FROM openjdk:11.0.1-slim

ADD build/distributions/auth-1.0.tar ./auth
WORKDIR ./auth
EXPOSE 8080

ENTRYPOINT ["bin/auth"]
