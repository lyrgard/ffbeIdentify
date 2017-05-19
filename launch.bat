mkdir bin
javac -d bin src/FfbeIdentify.java
jar -cmf src/manifest.mf ffbeIdentify.jar bin
java -jar ffbeIdentify.jar data
