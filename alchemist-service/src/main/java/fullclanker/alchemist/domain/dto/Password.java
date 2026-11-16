package fullclanker.alchemist.domain.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/** A raw password: at least 8 characters and at most 72 bytes in UTF-8 (bcrypt's limit). Null is valid. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Password.Validator.class)
public @interface Password {

	int MIN_CHARACTERS = 8;
	int MAX_BYTES = 72;

	String message() default "must be at least " + MIN_CHARACTERS + " characters and at most " + MAX_BYTES
			+ " bytes in UTF-8";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<Password, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return value == null || (value.codePointCount(0, value.length()) >= MIN_CHARACTERS
					&& value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES);
		}

	}

}
