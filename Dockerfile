FROM eclipse-temurin:17.0.16_8-jdk
RUN addgroup --system spring &&  adduser --system spring && adduser spring spring 
# USER spring:spring

ADD docker-entrypoint.sh /
COPY build/libs /app/
RUN chmod +x ./docker-entrypoint.sh
ENTRYPOINT ["/bin/sh","-c","exec /docker-entrypoint.sh"]
