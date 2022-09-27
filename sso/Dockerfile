FROM amazoncorretto:18
RUN yum install wget -y
RUN wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.jar

ADD build/distributions/sso.tar ./
COPY bootstrap.sh ./sso/
RUN chmod a+rw sso
WORKDIR ./sso
RUN chmod +x bootstrap.sh
EXPOSE 8080
EXPOSE 6001
EXPOSE 10001

ENTRYPOINT ["./bootstrap.sh"]
