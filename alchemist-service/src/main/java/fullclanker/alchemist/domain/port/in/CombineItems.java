package fullclanker.alchemist.domain.port.in;

import fullclanker.alchemist.domain.dto.CombineResult;

public interface CombineItems {

	/** Combines two items from the user's collection and adds the result to it. */
	CombineResult combine(String username, String nameA, String nameB);

}
