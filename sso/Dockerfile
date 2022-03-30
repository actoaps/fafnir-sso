FROM openjdk:17.0.2-jdk-oraclelinux8

RUN microdnf install -y findutils
ADD build/distributions/auth.tar ./
WORKDIR ./auth
EXPOSE 8080

ENTRYPOINT ["bin/sso"]
