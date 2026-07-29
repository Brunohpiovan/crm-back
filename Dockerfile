FROM openjdk:17
WORKDIR /app
COPY ./target/crm_vincit.jar /app
EXPOSE 8080
CMD ["java", "-jar", "crm_vincit.jar"]