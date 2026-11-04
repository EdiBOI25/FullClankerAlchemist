package lab.site.backend.adapter.in;

import lab.site.backend.domain.dto.ItemDto;
import lab.site.backend.domain.port.in.FindItems;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
public class ItemController {

	private final FindItems findItems;

	public ItemController(FindItems findItems) {
		this.findItems = findItems;
	}

	@GetMapping("/items")
	public List<ItemDto> findAll(
			@RequestParam(name = "tag", required = false)
			@Size(min = 1)
			List<@NotBlank @Size(max = 30) String> tags,
			HttpServletRequest request) {
		rejectArrayParameter(request);
		if (tags == null) {
			return findItems.findAll();
		}
		validateRawTags(request.getParameterValues("tag"));
		List<String> normalizedTags = tags.stream()
				.map(String::trim)
				.toList();
		return findItems.findByTags(normalizedTags);
	}

	private static void rejectArrayParameter(HttpServletRequest request) {
		if (request.getParameterValues("tag[]") != null) {
			throw new IllegalArgumentException("Tag fields must be valid");
		}
	}

	private static void validateRawTags(String[] rawTags) {
		List<String> values = rawTags.length == 1
				? List.of(rawTags[0].split(",", -1))
				: List.of(rawTags);
		if (values.stream().anyMatch(tag -> tag.isBlank() || tag.length() > 30)) {
			throw new IllegalArgumentException("Tag fields must be valid");
		}
	}

}
