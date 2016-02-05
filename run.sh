#!/bin/sh
mvn package
echo =====
echo ===== Running without agent
java -jar test/target/test-1.0.jar
echo ===== 
echo ===== Running with agent
java -javaagent:agent/target/agent-1.0-jar-with-dependencies.jar -jar test/target/test-1.0.jar
