package lab.site.backend.adapter.in;

import lab.site.backend.domain.dto.ItemDto;
import lab.site.backend.domain.port.in.FindItems;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ItemController {

	private final FindItems findItems;

	public ItemController(FindItems findItems) {
		this.findItems = findItems;
	}

	@GetMapping("/items")
	public List<ItemDto> findAll(@RequestParam(name = "tag", required = false) List<String> tags) {
		return tags == null ? findItems.findAll() : findItems.findByTags(tags);
	}

}
