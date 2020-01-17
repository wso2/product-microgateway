# Microgateway gRPC analytics details

Two files in this folder generated with the  Analytics proto file (Analytics.proto) and using the command

```
ballerina grpc --input Analytics.proto --output ./grpcAnalytics --mode client
```

It will generate sample files named
->Analytics_pb.bal
->AnalyticsSendService_sample_client.bal

Then they were renamed to,
Analytics_pb.bal                           -> gRPC_analytics_pb.bal
AnalyticsSendService_sample_client.bal     -> gRPC_analytics_client.bal
