FROM eclipse-temurin:22-alpine

COPY target/ipl-assignment-0.0.1.jar app.jar
EXPOSE 5000
ENV KAFKA_HOST=host.docker.internal:9094
ENV REDIS_HOST=host.docker.internal
ENV MYSQL_URL=jdbc:mysql://host.docker.internal:1000/ipl
ENTRYPOINT ["java", "-jar", "-Dspring.kafka.bootstrap-servers=${KAFKA_HOST}", "-Dspring.data.redis.host=${REDIS_HOST}", "-Dspring.datasource.url=${MYSQL_URL}", "/app.jar"]

#
#FROM eclipse-temurin:22-alpine
#
#COPY target/ipl-assignment-0.0.1.jar app.jar
#EXPOSE 5000
#ENV KAFKA_HOST=192.168.1.106:9094
#ENV REDIS_HOST=192.168.1.106
#ENV MYSQL_URL=jdbc:mysql://192.168.1.106:1000/ipl
#ENTRYPOINT ["java", "-jar", "-Dspring.kafka.bootstrap-servers=${KAFKA_HOST}", "-Dspring.data.redis.host=${REDIS_HOST}", "-Dspring.datasource.url=${MYSQL_URL}", "/app.jar"]
