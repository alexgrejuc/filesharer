FROM bellsoft/liberica-openjdk-alpine:17
WORKDIR /working-directory
COPY working-directory .
ENTRYPOINT ["java", "-jar", "filesharer.jar", "server"]
