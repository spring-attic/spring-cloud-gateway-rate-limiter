package org.springframework.cloud.gateway.ratelimiter;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class NoOpValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return true;
	}

	@Override
	public void validate(Object target, Errors errors) {
	}
}
