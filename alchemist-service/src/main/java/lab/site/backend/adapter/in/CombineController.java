package lab.site.backend.adapter.in;

import jakarta.validation.Valid;
import lab.site.backend.domain.dto.CombineRequest;
import lab.site.backend.domain.dto.CombineResult;
import lab.site.backend.domain.port.in.CombineItems;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CombineController {

	private final CombineItems combineItems;

	public CombineController(CombineItems combineItems) {
		this.combineItems = combineItems;
	}

	@PostMapping("/combine")
	public CombineResult combine(@Valid @RequestBody CombineRequest request) {
		return combineItems.combine(request.a(), request.b());
	}

}
