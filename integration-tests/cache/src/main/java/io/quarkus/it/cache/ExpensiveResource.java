package io.quarkus.it.cache;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.jboss.logging.Logger;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Path("/expensive-resource")
public class ExpensiveResource {

    private static final Logger LOGGER = Logger.getLogger(ExpensiveResource.class);

    private int invocations;

    @GET
    @Path("/{keyElement1}/{keyElement2}/{keyElement3}")
    @CacheResult(cacheName = "expensiveResourceCache", lockTimeout = 5000)
    public ExpensiveResponse getExpensiveResponse(@PathParam("keyElement1") @CacheKey String keyElement1,
            @PathParam("keyElement2") @CacheKey String keyElement2, @PathParam("keyElement3") @CacheKey String keyElement3,
            @QueryParam("foo") String foo) {
        invocations++;
        ExpensiveResponse response = new ExpensiveResponse();
        response.setResult(keyElement1 + " " + keyElement2 + " " + keyElement3 + " too!");
        return response;
    }

    @GET
    @Path("/invocations")
    public int getInvocations() {
        return invocations;
    }

    @GET
    @Path("/get1")
    @CacheResult(cacheName = "cache1")
    public String get1() {
        LOGGER.warn("get1 invoked");
        return "";
    }

    @GET
    @Path("/get2")
    @CacheResult(cacheName = "cache2")
    public String get2() {
        LOGGER.warn("get2 invoked");
        return "";
    }

    @DELETE
    @CacheInvalidateAll(cacheName = "cache1")
    @CacheInvalidateAll(cacheName = "cache2")
    public void delete() {
        LOGGER.warn("invalidation");
    }

    public static class ExpensiveResponse {

        private String result;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }
}
