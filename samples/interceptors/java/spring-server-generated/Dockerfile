FROM adoptopenjdk/openjdk11:jre-11.0.11_9-alpine
RUN addgroup -S wso2am && adduser -S wso2am -G wso2am
USER wso2am:wso2am

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} interceptor-svc.jar

EXPOSE 9081
ENTRYPOINT ["java","-jar","/interceptor-svc.jar"]
