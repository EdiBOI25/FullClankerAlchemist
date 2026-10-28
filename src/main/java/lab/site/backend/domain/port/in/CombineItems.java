package lab.site.backend.domain.port.in;

import lab.site.backend.domain.dto.CombineResult;

public interface CombineItems {

	CombineResult combine(String nameA, String nameB);

}
