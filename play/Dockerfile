FROM openjdk:8-jre

RUN mkdir /svc/
WORKDIR /svc/
ADD ./svc/ /svc/

EXPOSE 9000 9443
CMD [ "/svc/bin/start", "-Dhttps.port=9443", "-Dplay.http.secret.key=secret" ]
