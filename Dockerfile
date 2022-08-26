FROM eclipse-temurin:17-jdk

COPY build/libs/kotik-bot-0.0.1-SNAPSHOT.jar /service.jar

USER 999
ENTRYPOINT ["java", "-jar", "/service.jar"]
