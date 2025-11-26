package paymentservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public int getRandomNumber() {
        // На сайте у них указано http, а не https. Но вроде и https работает без проблем
        String url = "https://www.randomnumberapi.com/api/v1.0/random?min=1&max=2";

        Integer[] response = restTemplate.getForObject(url, Integer[].class);

        if (response == null || response.length == 0) {
            throw new IllegalStateException("Random number API returned null");
        }

        return response[0];
    }
}
