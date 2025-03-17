FROM eclipse-temurin:17-jdk
LABEL authors="jonathandepaula"

ENV INSTANCE_ID=${INSTANCE_ID}

COPY target/dynamicallytaskscheduler-0.0.1-SNAPSHOT.jar /dynamicallytaskscheduler.jar

ENTRYPOINT ["java", "-jar", "/dynamicallytaskscheduler.jar"]