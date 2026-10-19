package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;

public record BranchOrderInformation(String branchOrderId, ActivityType activityType, String thirdPartyNationalId) {

}
