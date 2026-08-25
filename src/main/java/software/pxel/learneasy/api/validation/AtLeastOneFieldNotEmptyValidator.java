package software.pxel.learneasy.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

public class AtLeastOneFieldNotEmptyValidator implements ConstraintValidator<AtLeastOneFieldNotEmpty, Object> {

    private String[] fields;

    @Override
    public void initialize(AtLeastOneFieldNotEmpty constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);

        for (String fieldName : fields) {
            Object fieldValue = beanWrapper.getPropertyValue(fieldName);

            if (fieldValue != null) {
                if (fieldValue instanceof String && !((String) fieldValue).isBlank()) {
                    return true;
                }
                if (!(fieldValue instanceof String)) {
                    return true;
                }
            }
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        "At least one of fields " + String.join(", ", fields) + " must be provided.")
                .addConstraintViolation();

        return false;
    }
}
