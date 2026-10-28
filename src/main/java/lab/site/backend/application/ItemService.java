package lab.site.backend.application;

import lab.site.backend.domain.dto.ItemDto;
import lab.site.backend.domain.port.in.FindItems;
import lab.site.backend.domain.port.out.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService implements FindItems {

	private final ItemRepository items;

	public ItemService(ItemRepository items) {
		this.items = items;
	}

	@Override
	public List<ItemDto> findAll() {
		return items.findAllByOrderByIdAsc().stream()
				.map(ItemDto::from)
				.toList();
	}

}
