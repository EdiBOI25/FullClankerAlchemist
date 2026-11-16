package fullclanker.alchemist.domain.port.in;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.model.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CreateUser {

	void create(@NotNull @Valid Credentials credentials, @NotNull Role role);

}
