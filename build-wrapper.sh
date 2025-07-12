#\!/bin/bash
export JAVA_HOME="/mnt/c/Program Files/Java/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"
echo "Java version:"
"$JAVA_HOME/bin/java.exe" -version
echo "Starting Gradle build..."
./gradlew createDistribution
EOF < /dev/null
