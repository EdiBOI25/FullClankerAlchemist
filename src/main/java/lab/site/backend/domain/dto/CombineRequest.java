package lab.site.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CombineRequest(
		@NotBlank @Size(max = 50) String a,
		@NotBlank @Size(max = 50) String b) {
}
