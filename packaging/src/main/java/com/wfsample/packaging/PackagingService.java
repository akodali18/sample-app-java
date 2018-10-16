package com.wfsample.packaging;

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;
import com.wavefront.sdk.grpc.WavefrontServerTracerFactory;
import com.wavefront.sdk.grpc.reporter.WavefrontGrpcReporter;
import com.wfsample.beachshirts.PackagingGrpc;
import com.wfsample.beachshirts.PackedShirts;
import com.wfsample.beachshirts.WrapRequest;
import com.wfsample.common.BeachShirtsUtils;
import com.wfsample.common.GrpcServiceConfig;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

/**
 * Driver for the packaging service which packs the a given shirt.
 *
 * @author Srujan Narkedamalli (snarkedamall@wavefront.com).
 */
public class PackagingService {

  public PackagingService(GrpcServiceConfig config) throws Exception {
    WavefrontSender sender = BeachShirtsUtils.getWavefrontSender(config.getWavefrontReporting());
    ApplicationTags tags = BeachShirtsUtils.getApplicationTags(config.getMetadata());
    WavefrontGrpcReporter grpcReporter = new WavefrontGrpcReporter.Builder(tags).
        withSource(config.getWavefrontReporting().getSource()).
        reportingIntervalSeconds(config.getWavefrontReporting().getFlushIntervalSeconds()).
        build(sender);
    grpcReporter.start();
    WavefrontServerTracerFactory tracerFactory =
        new WavefrontServerTracerFactory(grpcReporter, tags, true);
    ServerBuilder builder = ServerBuilder.forPort(config.getGrpcPort()).
        addService(new PackagingImpl()).addStreamTracerFactory(tracerFactory);
    Server packaging = builder.build();
    System.out.println("Starting Packaging server ...");
    packaging.start();
    System.out.println("Packaging server started");
    packaging.awaitTermination();
  }

  public static void main(String[] args) throws Exception {
    GrpcServiceConfig configuration = BeachShirtsUtils.scenarioFromFile(args[0]);
    new PackagingService(configuration);
  }

  static class PackagingImpl extends PackagingGrpc.PackagingImplBase {

    @Override
    public void wrapShirts(WrapRequest request, StreamObserver<PackedShirts> responseObserver) {
      responseObserver.onNext(PackedShirts.newBuilder().
          addAllShirts(request.getShirtsList()).
          build());
      responseObserver.onCompleted();
    }
  }
}
