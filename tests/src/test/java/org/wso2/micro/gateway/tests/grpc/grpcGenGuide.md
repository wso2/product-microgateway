Use the following command to generate the code from the test.proto

protoc --plugin=protoc-gen-grpc-java=path/to/protoc-gen-grpc-java --grpc-java_out=outputFilePath 
--java_out=outputFilePath test.proto
