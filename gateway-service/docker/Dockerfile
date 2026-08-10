FROM amazoncorretto:21

ARG JAR_FILE=build/libs/gateway-service.jar
COPY ${JAR_FILE} .

ARG SPRINGBOOT_APP_PROFILE
ENV PROFILE=${SPRINGBOOT_APP_PROFILE}

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${PROFILE}", "gateway-service.jar"]
