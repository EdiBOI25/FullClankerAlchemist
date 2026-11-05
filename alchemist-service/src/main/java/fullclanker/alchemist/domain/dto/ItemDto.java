package fullclanker.alchemist.domain.dto;

import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Tag;

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
