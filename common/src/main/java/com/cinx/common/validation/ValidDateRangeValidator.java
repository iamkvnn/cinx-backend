package com.cinx.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startDateField;
    private String dueDateField;

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startDateField = constraintAnnotation.startDateField();
        this.dueDateField = constraintAnnotation.dueDateField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            Field startField = value.getClass().getDeclaredField(startDateField);
            startField.setAccessible(true);
            LocalDateTime startDate = (LocalDateTime) startField.get(value);

            Field dueField = value.getClass().getDeclaredField(dueDateField);
            dueField.setAccessible(true);
            LocalDateTime dueDate = (LocalDateTime) dueField.get(value);

            if (startDate != null && dueDate != null) {
                return startDate.isBefore(dueDate);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
