package lab.site.backend.domain.port.in;

import lab.site.backend.domain.dto.ItemDto;

import java.util.List;

public interface FindItems {

	List<ItemDto> findAll();

}
