package fullclanker.alchemist.domain.port.in;

import fullclanker.alchemist.domain.dto.CombineResult;

public interface CombineItems {

	CombineResult combine(String nameA, String nameB);

}
