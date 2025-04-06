package com.se498.chat;

import com.se498.chat.model.ChatMessage;
import com.se498.chat.repository.MessageRepository;
import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.json.JSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.util.UUID;

@SpringBootApplication
@EntityScan(basePackages = {"com.se498.chat.model"})
public class ChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(MessageRepository service) {
		return args -> {
			System.out.println("Let's inspect the beans provided by Spring Boot:");

			ChatMessage dummy = service.save(new ChatMessage(UUID.randomUUID().toString(), "test", "testing", 111111));

			System.out.println("This is a return Relational " + dummy.getMessageText());

		 	AwsCredentialsProvider provider = DefaultCredentialsProvider.create();
			AwsCredentials creds = provider.resolveCredentials();
			System.out.println(creds.accessKeyId());
			System.out.println(creds.secretAccessKey());

			var client = BedrockRuntimeClient.builder()
					.credentialsProvider(DefaultCredentialsProvider.create())
					.region(Region.US_EAST_1)
					.build();

			// Set the model ID, e.g., Claude 3 Haiku.
			var modelId = "anthropic.claude-3-haiku-20240307-v1:0";

			var nativeRequestTemplate = """
                {
                    "anthropic_version": "bedrock-2023-05-31",
                    "max_tokens": 512,
                    "temperature": 0.5,
                    "messages": [{
                        "role": "user",
                        "content": "{{prompt}}"
                    }]
                }""";

			// Define the prompt for the model.
			var prompt = "Describe the purpose of a 'hello world' program in one line.";

			// Embed the prompt in the model's native request payload.
			String nativeRequest = nativeRequestTemplate.replace("{{prompt}}", prompt);

			try {
				// Encode and send the request to the Bedrock Runtime.
				var response = client.invokeModel(request -> request
						.body(SdkBytes.fromUtf8String(nativeRequest))
						.modelId(modelId)
				);

				// Decode the response body.
				var responseBody = new JSONObject(response.body().asUtf8String());

				System.out.println(responseBody.get("content"));

			} catch (SdkClientException e) {
				System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", modelId, e.getMessage());
				throw new RuntimeException(e);
			}
			ChatLanguageModel model = BedrockAnthropicMessageChatModel
					.builder()
					.credentialsProvider(DefaultCredentialsProvider.create())
					.temperature(0.50f)
					.maxTokens(300)
					.region(Region.US_EAST_1)
					.model(BedrockAnthropicMessageChatModel.Types.AnthropicClaude3SonnetV1.getValue())
					.maxRetries(1)
					// Other parameters can be set as well
					.build();

			String joke = model.generate("Tell me a joke about Java");

			System.out.println(joke);
		};
	}
}
