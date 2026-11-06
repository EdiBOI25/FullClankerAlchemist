package fullclanker.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateRequest(
		@NotBlank @Size(max = 50) String a,
		@NotBlank @Size(max = 50) String b) {
}
