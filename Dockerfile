FROM eclipse-temurin:17-jdk
LABEL authors="jonathandepaula"

ENV INSTANCE_ID=${INSTANCE_ID}

COPY . /app
WORKDIR /app
RUN ./mvnw clean package -DskipTests

ENTRYPOINT ["java", "-jar", "target/dynamicallytaskscheduler-0.0.1-SNAPSHOT.jar"]