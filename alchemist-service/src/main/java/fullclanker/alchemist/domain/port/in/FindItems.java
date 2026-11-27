package fullclanker.alchemist.domain.port.in;

import fullclanker.alchemist.domain.dto.ItemDto;

import java.util.Collection;
import java.util.List;

public interface FindItems {

	/** The user's collection, in discovery order. */
	List<ItemDto> findAll(String username);

	/** The items in the user's collection that have every tag. */
	List<ItemDto> findByTags(String username, Collection<String> tags);

}
