export SRC_DIR=usvm-net/src/test
export JAVA_DST=usvm-net/src/test/java/
export KOTLIN_DST=usvm-net/src/test/kotlin/
protoc -I=$SRC_DIR --java_out=$JAVA_DST --kotlin_out=$KOTLIN_DST $SRC_DIR/test-expressions.proto
