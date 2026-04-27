package org.example.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.example.api.dto.HealthResponse;

@Path("/v1/health")
@Produces(MediaType.APPLICATION_JSON)
public class V1HealthResource {

    @GET
    public HealthResponse health() {
        return new HealthResponse("UP", "local-resources", "1");
    }
}
