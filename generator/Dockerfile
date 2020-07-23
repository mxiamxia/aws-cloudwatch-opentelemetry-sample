# this dockerfile is used for github action
FROM gradle:6.5.0-jdk8 as builder

# build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build

# settle down the executable
FROM adoptopenjdk:8-jdk-hotspot
COPY --from=builder /home/gradle/src/build/distributions/generator-1.0-SNAPSHOT.tar /app/
WORKDIR /app
RUN tar -xvf generator-1.0-SNAPSHOT.tar

# run
ENTRYPOINT /app/generator-1.0-SNAPSHOT/bin/generator