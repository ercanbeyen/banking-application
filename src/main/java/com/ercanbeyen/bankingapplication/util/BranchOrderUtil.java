package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@UtilityClass
@Slf4j
public class BranchOrderUtil {
    private final List<Integer> VALID_MONTH_FILTERS = List.of(1, 3, 6);
    private final List<String> permittedFileFormatsForForPetition = List.of(MediaType.APPLICATION_PDF_VALUE, FileUtil.APPLICATION_DOC_VALUE, FileUtil.APPLICATION_DOCX_VALUE);

    public void checkRequest(BranchOrderDto request, MultipartFile file) {
        if (!ActivityType.getBranchOrderActivities().contains(request.activityType())) {
            throw new BadRequestException("Invalid activity type!");
        }

        if (Optional.ofNullable(file).isPresent()) {
            if (file.isEmpty()) {
                throw new BadRequestException("Petition file is empty!");
            }

            if (!permittedFileFormatsForForPetition.contains(file.getContentType())) {
                throw new BadRequestException("Invalid file format! Permitted file formats: " + permittedFileFormatsForForPetition);
            }
        } else if (StringUtils.isBlank(request.content())) {
            throw new BadRequestException("No content or petition is submitted!");
        }
    }

    public void checkBranchOrderStatusUpdate(BranchOrderStatus status, String accountActivityId) {
        String entity = Entity.BRANCH_ORDER.getValue();

        if (!TimeUtil.isLocalDateTimeInWorkDaysAndHours(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw new BadRequestException(String.format("%ss may be processed on business days and hours.", entity));
        }

        if (status == BranchOrderStatus.PENDING || status == BranchOrderStatus.CANCELED) {
            throw new BadRequestException(String.format("Invalid %s status update for the teller", entity.toLowerCase()));
        }

        if (status == BranchOrderStatus.COMPLETED && Optional.ofNullable(accountActivityId).isEmpty()) {
            throw new BadRequestException(String.format("%s id is empty!", Entity.ACCOUNT_ACTIVITY.getValue()));
        }
    }

    public void checkRequestParametersForGetBranchOrders(Integer fromMonth) {
        if (Optional.ofNullable(fromMonth).isEmpty()) {
            log.warn("No month is specified!");
            return;
        }

        if (!BranchOrderUtil.VALID_MONTH_FILTERS.contains(fromMonth)) {
            throw new BadRequestException("Invalid month!");
        }
    }
}
