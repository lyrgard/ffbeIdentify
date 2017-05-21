rm bin/*
mkdir bin
javac -d bin -cp ".:lib/javax.json-1.1.jar:lib/javax.json-api-1.1.jar" src/FfbeIdentify.java
cd bin
java -cp ".:../lib/javax.json-1.1.jar:../lib/javax.json-api-1.1.jar" FfbeIdentify ../data
