package com.ercanbeyen.bankingapplication.embeddable;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@UserDefinedType
public class Rating {
    private String title;
    private Integer rate;
}
