export SRC_DIR=/home/rnpozharskiy/work/usvm/usvm-net/src/test
export DST_DIR=/home/rnpozharskiy/work/usvm/usvm-net/src/test/kotlin/executor
protoc -I=$SRC_DIR --java_out=$DST_DIR/generated/java --kotlin_out=$DST_DIR/generated/kotlin $SRC_DIR/test.proto
