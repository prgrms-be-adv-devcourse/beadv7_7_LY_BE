FROM amazoncorretto:21

ARG JAR_FILE=build/libs/fulfillment-service.jar
COPY ${JAR_FILE} .

ARG SPRINGBOOT_APP_PROFILE
ENV PROFILE=${SPRINGBOOT_APP_PROFILE}

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${PROFILE}", "fulfillment-service.jar"]
