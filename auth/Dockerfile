FROM openjdk:13-jdk-oraclelinux7

ADD build/distributions/auth.tar ./
WORKDIR ./auth
EXPOSE 8080

ENTRYPOINT ["bin/auth"]
