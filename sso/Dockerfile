FROM amazoncorretto:17

RUN yum update -y && yum install wget -y
RUN wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.jar

ADD build/distributions/sso.tar ./
COPY bootstrap.sh ./sso/
RUN chmod a+rw sso
WORKDIR ./sso
RUN chmod +x bootstrap.sh
EXPOSE 8080
EXPOSE 6001

ENTRYPOINT ["./bootstrap.sh"]
