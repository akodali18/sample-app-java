package com.wfsample.styling;

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;
import com.wavefront.sdk.grpc.WavefrontClientInterceptor;
import com.wavefront.sdk.grpc.reporter.WavefrontGrpcReporter;
import com.wavefront.sdk.jersey.WavefrontJerseyFilter;
import com.wavefront.sdk.jersey.reporter.WavefrontJerseyReporter;
import com.wfsample.beachshirts.PackagingGrpc;
import com.wfsample.beachshirts.PrintRequest;
import com.wfsample.beachshirts.PrintingGrpc;
import com.wfsample.beachshirts.Shirt;
import com.wfsample.beachshirts.ShirtStyle;
import com.wfsample.beachshirts.WrapRequest;
import com.wfsample.common.BeachShirtsUtils;
import com.wfsample.common.DropwizardServiceConfig;
import com.wfsample.common.dto.OrderStatusDTO;
import com.wfsample.common.dto.ShirtStyleDTO;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Driver for styling service which manages different styles of shirts and takes orders for a shirts
 * of a given style.
 *
 * @author Srujan Narkedamalli (snarkedamall@wavefront.com).
 */
public class StylingService extends Application<DropwizardServiceConfig> {

  private StylingService() {
  }

  public static void main(String[] args) throws Exception {
    new StylingService().run(args);
  }

  @Override
  public void run(DropwizardServiceConfig config, Environment environment)
      throws Exception {
    WavefrontSender sender = BeachShirtsUtils.getWavefrontSender(config.getWavefrontReporting());
    ApplicationTags tags = BeachShirtsUtils.getApplicationTags(config.getMetadata());
    WavefrontGrpcReporter grpcReporter = new WavefrontGrpcReporter.Builder(tags).
        withSource(config.getWavefrontReporting().getSource()).
        reportingIntervalSeconds(config.getWavefrontReporting().getFlushIntervalSeconds()).
        build(sender);
    grpcReporter.start();
    WavefrontJerseyReporter jerseyReporter = new WavefrontJerseyReporter.Builder(tags).
        withSource(config.getWavefrontReporting().getSource()).
        reportingIntervalSeconds(config.getWavefrontReporting().getFlushIntervalSeconds()).
        build(sender);
    jerseyReporter.start();
    WavefrontJerseyFilter jerseyFilter = new WavefrontJerseyFilter(jerseyReporter, tags);
    WavefrontClientInterceptor interceptor =
        new WavefrontClientInterceptor(grpcReporter, tags, true);
    environment.jersey().register(jerseyFilter);
    environment.jersey().register(new StylingWebResource(config, interceptor));
  }

  @Path("/style")
  @Produces(MediaType.APPLICATION_JSON)
  public class StylingWebResource {
    // sample set of static styles.
    private List<ShirtStyleDTO> shirtStyleDTOS = new ArrayList<>();
    private final PrintingGrpc.PrintingBlockingStub printing;
    private final PackagingGrpc.PackagingBlockingStub packaging;

    public StylingWebResource(DropwizardServiceConfig config, ClientInterceptor interceptor) {
      ShirtStyleDTO dto = new ShirtStyleDTO();
      dto.setName("style1");
      dto.setImageUrl("style1Image");
      ShirtStyleDTO dto2 = new ShirtStyleDTO();
      dto2.setName("style2");
      dto2.setImageUrl("style2Image");
      shirtStyleDTOS.add(dto);
      shirtStyleDTOS.add(dto2);
      ManagedChannel printingChannel = ManagedChannelBuilder.forAddress("localhost",
          config.getPrintingPort()).intercept(interceptor).usePlaintext().build();
      ManagedChannel packagingChannel = ManagedChannelBuilder.forAddress("localhost",
          config.getPackagingPort()).intercept(interceptor).usePlaintext().build();
      printing = PrintingGrpc.newBlockingStub(printingChannel);
      packaging = PackagingGrpc.newBlockingStub(packagingChannel);

    }

    @GET
    public Response getAllStyles() {
      return Response.ok(shirtStyleDTOS).build();
    }

    @GET
    @Path("{id}/make")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response makeShirts(@PathParam("id") String id, @QueryParam("quantity") int quantity) {
      Iterator<Shirt> shirts = printing.printShirts(PrintRequest.newBuilder().
          setStyleToPrint(ShirtStyle.newBuilder().setName(id).setImageUrl(id + "Image").build()).
          setQuantity(quantity).build());
      packaging.wrapShirts(WrapRequest.newBuilder().addAllShirts(() -> shirts).build());
      return Response.ok(new OrderStatusDTO("completed")).build();
    }
  }
}
