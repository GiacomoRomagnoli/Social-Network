FROM openjdk:21-jdk AS build
WORKDIR /Social-Network
COPY ./build/ /Social-Network/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "./libs/notification-service.jar"]