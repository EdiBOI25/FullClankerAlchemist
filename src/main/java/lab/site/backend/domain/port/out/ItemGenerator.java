package lab.site.backend.domain.port.out;

import lab.site.backend.domain.dto.GeneratedItem;

public interface ItemGenerator {

	GeneratedItem generate(String nameA, String nameB);

}
