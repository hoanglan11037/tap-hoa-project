package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.Set;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

/**
 * Kiểm tra các ràng buộc khai báo trên lớp request (@NotBlank, @Pattern, @Size...) trước khi ghi CSDL.
 * Giao diện đã kiểm tra tại chỗ; đây là lớp bảo vệ cuối ở tầng service.
 */
public final class Validation {

    private static final Validator VALIDATOR = jakarta.validation.Validation.byProvider(HibernateValidator.class)
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    private Validation() {
    }

    /** Ném BusinessException với thông báo của lỗi đầu tiên (theo tên trường) nếu dữ liệu không hợp lệ. */
    public static void check(Object request) {
        Set<ConstraintViolation<Object>> errors = VALIDATOR.validate(request);
        errors.stream()
                .min(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .ifPresent(v -> {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, v.getMessage());
                });
    }
}
