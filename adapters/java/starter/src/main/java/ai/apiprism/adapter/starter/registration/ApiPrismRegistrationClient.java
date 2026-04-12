package ai.apiprism.adapter.starter.registration;

import ai.apiprism.adapter.starter.exceptions.ApiPrismRegistrationException;
import ai.apiprism.protocol.registration.ApiRegistrationRequest;
import ai.apiprism.protocol.registration.ApiRegistrationResponse;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;

/**
 * 向 APIPrism center 发送注册请求的 HTTP 客户端。
 */
public class ApiPrismRegistrationClient {

    private final RestClient restClient;

    public ApiPrismRegistrationClient() {
        this(RestClient.builder().build());
    }

    public ApiPrismRegistrationClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public ApiRegistrationResponse register(URI centerBaseUrl, ApiRegistrationRequest request) {
        try {
            ApiRegistrationResponse response = restClient.post()
                    .uri(resolveRegistrationEndpoint(centerBaseUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ApiRegistrationResponse.class);
            if (response == null) {
                throw new ApiPrismRegistrationException(
                    "Center registration failed with empty response body.", false);
            }
            return response;
        } catch (RestClientResponseException exception) {
            boolean retryable = exception.getStatusCode().is5xxServerError();
            throw new ApiPrismRegistrationException(
                    "Center registration failed with status "
                            + exception.getStatusCode().value()
                            + ": "
                            + exception.getResponseBodyAsString(),
                    exception,
                    retryable
            );
        } catch (RestClientException exception) {
            throw new ApiPrismRegistrationException(
                    "Unable to register with APIPrism center", exception, true);
        }
    }

    static URI resolveRegistrationEndpoint(URI centerBaseUrl) {
        String base = centerBaseUrl.toString();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return URI.create(base + "api/v1/registrations");
    }
}
