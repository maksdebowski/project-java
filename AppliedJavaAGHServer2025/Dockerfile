FROM eclipse-temurin:21-jdk
RUN mkdir /config
WORKDIR /app
ADD target/mazev-server-1.0-SNAPSHOT-allinone.jar mazev-server-1.0-SNAPSHOT-allinone.jar
EXPOSE 8080
WORKDIR /
CMD java -XX:+PrintFlagsFinal -Xmx450m -jar app/mazev-server-1.0-SNAPSHOT-allinone.jar