package lab.site.backend.domain.dto;

import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Tag;

import java.util.List;

public record ItemDto(Long id, String name, String emoji, String iconUrl, List<String> tags) {

	public static ItemDto from(Item item) {
		List<String> tagNames = item.getTags().stream()
				.map(Tag::getName)
				.sorted()
				.toList();
		return new ItemDto(item.getId(), item.getName(), item.getEmoji(), item.getIconUrl(), tagNames);
	}

}
