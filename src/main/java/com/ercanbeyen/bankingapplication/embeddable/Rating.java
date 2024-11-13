package com.ercanbeyen.bankingapplication.embeddable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@UserDefinedType
public class Rating {
    @NotNull(message = "Title should not be null")
    @NotBlank(message = "Title should not be blank")
    private String title;
    @Range(min = 1, max = 5, message = "Rate should be between {min} and {max}")
    private Integer rate;
}
