package fullclanker.ai.controller;

import fullclanker.ai.dto.GenerateRequest;
import fullclanker.ai.dto.GeneratedItem;
import fullclanker.ai.service.ChatClientItemGenerator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GenerateController {

	private final ChatClientItemGenerator generator;

	public GenerateController(ChatClientItemGenerator generator) {
		this.generator = generator;
	}

	@PostMapping("/generate")
	public GeneratedItem generate(@Valid @RequestBody GenerateRequest request) {
		return generator.generate(request.a(), request.b());
	}

}
