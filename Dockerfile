FROM openjdk:8-jre

#################################################################
WORKDIR /
RUN apt update
RUN apt install ruby-dev -y 
RUN apt install git -y
RUN apt install build-essential -y
RUN git clone https://github.com/urbanadventurer/urlcrazy.git
WORKDIR urlcrazy/
RUN gem install bundler
RUN bundle install
WORKDIR /
#################################################################

#Copy the war.
ARG JAR_FILE=target/app.jar
COPY ${JAR_FILE} app.jar

#Setup the default log4j2.conf.
ARG LOG4J2_FILE=src/main/resources/log4j2-spring.xml
COPY ${LOG4J2_FILE} /conf/log4j2.xml

CMD ["java", "-Dlogging.config=/conf/log4j2.xml","-jar","/app.jar"]
