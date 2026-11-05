package fullclanker.alchemist.domain.port.in;

import fullclanker.alchemist.domain.dto.ItemDto;

import java.util.Collection;
import java.util.List;

public interface FindItems {

	List<ItemDto> findAll();

	List<ItemDto> findByTags(Collection<String> tags);

}
