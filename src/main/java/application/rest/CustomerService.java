package application.rest;

import model.Customer;
import application.rest.client.CloudantClientService;

import javax.enterprise.context.RequestScoped;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/customer")
@RequestScoped
@Produces("application/json")
@OpenAPIDefinition(
    info = @Info(
        title = "Customer Service",
        version = "0.0",
        description = "getCustomer API",
        contact = @Contact(url = "https://github.com/ibm-cloud-architecture", name = "IBM CASE"),
        license = @License(name = "License", url = "https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-customer/blob/master/LICENSE")
        )
    )
public class CustomerService {

    @Inject
    private JsonWebToken jwt;

    @Inject
    @RestClient
    private CloudantClientService defaultCloudantClient;


    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, maxDuration= 2000)
    @Fallback(fallbackMethod = "fallbackService")
    //@Produces("application/json")
    @GET
    @APIResponses(value = {
        @APIResponse(
            responseCode = "404",
            description = "The cloudant database cannot be fround. ",
            content = @Content(
                        mediaType = "text/plain")),
        @APIResponse(
            responseCode = "200",
            description = "The Customer Data has been retrieved successfully.",
            content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Customer.class)))})
    @Operation(
        summary = "Get customer by jwt username.",
        description = "Retrieves the customer informtation for the jwt subject."
    )
    @Timed(name = "Customer.timer",
            absolute = true,
            displayName="Customer Timer",
            description = "Time taken by the Customer Service")
    @Counted(name="Customer",
            absolute = true,
            displayName="Customer Call count",
            description="Number of times the Customer call happened.",
            monotonic=true)
    @Metered(name="CustomerMeter",
            displayName="Customer Call Frequency",
            description="Rate of the calls made to CloudantDB")
    @Traced(value = true, operationName = "getCustByUsername")
    public javax.ws.rs.core.Response getCustomerByUsername() throws Exception{
        try {
            String username = "usernames:" + jwt.getSubject();
            return defaultCloudantClient.getCustomerByUsername(username, "true");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build());
            throw new Exception(e.toString());
        }
    }

    @Produces("application/json")
    @GET
    public javax.ws.rs.core.Response fallbackService() {
        System.out.println();
        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity("Cloudant Service is down.").build();
    }

}
