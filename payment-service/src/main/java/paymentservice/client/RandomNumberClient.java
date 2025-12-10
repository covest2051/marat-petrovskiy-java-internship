package paymentservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String RANDOM_NUMBER_API_URL = "https://www.randomnumberapi.com/api/v1.0/random?min=1&max=2";

    public int getRandomNumber() {
        Integer[] response = restTemplate.getForObject(RANDOM_NUMBER_API_URL, Integer[].class);

        if (response == null || response.length == 0) {
            throw new IllegalStateException("Random number API returned null");
        }

        return response[0];
    }
}
