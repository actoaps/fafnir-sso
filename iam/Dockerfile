FROM eclipse-temurin:17-jre

RUN apt-get install wget -y
RUN wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.jar

ADD build/distributions/iam.tar ./
COPY bootstrap.sh ./iam/
RUN chmod a+rw iam
WORKDIR ./iam
RUN chmod +x bootstrap.sh
EXPOSE 8080
EXPOSE 6001

ENTRYPOINT ["./bootstrap.sh"]
