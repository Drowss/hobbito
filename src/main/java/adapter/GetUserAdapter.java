package adapter;

import adapter.response.UserResponse;
import common.HobbaException;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public class GetUserAdapter {
    private final WebClient webClient;
    private final Dotenv dotenv;

    public GetUserAdapter(Dotenv  dotenv) {
        this.webClient = WebClient
                .builder()
                .baseUrl("https://api.hobba.tv")
                .build();
        this.dotenv = dotenv;
    }

    public Mono<UserResponse> getUser(String username) {
        return webClient.get()
                .uri("/api/user/{username}", username)
                .header("api-key", dotenv.get("HOBBA_API_KEY"))
                .exchangeToMono(this::handleResponse)
                .doOnSubscribe(subscription -> log.info("Getting {}", username))
                .doOnError(error -> log.error("getting user failed {} error: {}", username, error.getMessage()));
    }

    private Mono<UserResponse> handleResponse(ClientResponse response) {
        String contentType = response.headers()
                .contentType()
                .map(ct -> ct.toString())
                .orElse("");

        if (response.statusCode() == HttpStatus.OK && contentType.contains("application/json")) {
            return response.bodyToMono(UserResponse.class)
                    .doOnNext(res -> log.info("User found {}", res.getUsername()));
        } else if(response.statusCode() == HttpStatus.OK && contentType.contains("text/html")) {
            return Mono.error(new HobbaException("No encontramos al usuario en ninguna habitación del hotel,\n " +
                    "¿estás seguro que existe?"));
        }
        return Mono.error(new RuntimeException("Algo salió mal, lo estamos revisando."));
    }
}
