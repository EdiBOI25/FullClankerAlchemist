package fullclanker.alchemist.domain.port.out;

import fullclanker.alchemist.domain.dto.GeneratedItem;

public interface ItemGenerator {

	GeneratedItem generate(String nameA, String nameB);

}
