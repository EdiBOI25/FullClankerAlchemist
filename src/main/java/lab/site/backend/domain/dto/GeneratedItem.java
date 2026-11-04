package lab.site.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GeneratedItem(
		@NotBlank @Size(max = 50) String name,
		@NotBlank @Size(max = 16) String emoji,
		@NotEmpty @Size(max = 5) List<@NotBlank @Size(max = 30) String> tags) {
}
