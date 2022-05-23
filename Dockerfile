FROM openapitools/openapi-generator-cli:v5.4.0
WORKDIR /awesome-generators
# Awesome Generators
COPY target/awesome-generators-1.0.0-jar-with-dependencies.jar ./awesome-generators.jar
ENV CLASSPATH=$CLASSPATH:/opt/openapi-generator/modules/openapi-generator-cli/target/openapi-generator-cli.jar
ENV CLASSPATH=$CLASSPATH:/awesome-generators/awesome-generators.jar
