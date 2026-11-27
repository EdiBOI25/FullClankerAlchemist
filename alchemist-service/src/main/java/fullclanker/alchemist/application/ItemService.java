package fullclanker.alchemist.application;

import fullclanker.alchemist.domain.dto.ItemDto;
import fullclanker.alchemist.domain.port.in.FindItems;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
public class ItemService implements FindItems {

	private final ItemRepository items;

	public ItemService(ItemRepository items) {
		this.items = items;
	}

	@Override
	public List<ItemDto> findAll(String username) {
		return items.findAllOwnedBy(username).stream()
				.map(ItemDto::from)
				.toList();
	}

	@Override
	public List<ItemDto> findByTags(String username, Collection<String> tags) {
		List<String> wanted = tags.stream()
				.map(tag -> tag.toLowerCase(Locale.ROOT))
				.toList();
		// Shortcut: filters every item in memory; move to a JPQL query if the collection grows large
		return findAll(username).stream()
				.filter(item -> item.tags().containsAll(wanted))
				.toList();
	}

}
