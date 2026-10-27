package lab.site.backend.config;

import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Tag;
import lab.site.backend.domain.port.out.ItemRepository;
import lab.site.backend.domain.port.out.TagRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class BaseItemSeeder implements CommandLineRunner {

	private final ItemRepository items;
	private final TagRepository tags;

	public BaseItemSeeder(ItemRepository items, TagRepository tags) {
		this.items = items;
		this.tags = tags;
	}

	@Override
	@Transactional
	public void run(String... args) {
		seed("fire", "🔥", "hot");
		seed("water", "💧", "fluid");
		seed("earth", "🌍", "solid");
		seed("air", "💨", "gas");
	}

	private void seed(String itemName, String emoji, String tagName) {
		if (items.findByNameIgnoreCase(itemName).isPresent()) {
			return;
		}

		Tag tag = tags.findByName(tagName)
				.orElseGet(() -> tags.save(new Tag(tagName)));
		items.save(new Item(itemName, emoji, Set.of(tag)));
	}

}
