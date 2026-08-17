package com.ercanbeyen.bankingapplication.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentType {
    RESIDENTIAL_RENT("Residential Rent"),
    BUSINESS_RENT("Business Rent"),
    OTHER_RENT("Other Rent"),
    E_COMMERCE("E-Commerce"),
    PERSONAL_PAYMENT("Personal Payment"),
    EMPLOYEE_PAYMENT("Employee Payment"),
    PENSION_PAYMENT("Pension Payment"),
    INVESTMENT("Investment"),
    FINANCIAL("Financial"),
    TUITION_PAYMENT("Tuition Payment"),
    SUBSCRIPTION_FEE("Subscription Fee"),
    OTHER("Other"),
    REAL_ESTATE_PURCHASE_PAYMENT("Real Estate Purchase Payment"),
    MOTOR_VEHICLE_PURCHASE_PAYMENT("Motor Vehicle Purchase Payment"),
    LENDING_DEBT_PAYMENT("Lending/Debt Payment"),
    GIFT_DONATION_AID_PAYMENT("Gift/Donation/Aid Payment"),
    TAX_DUTY_FEE_PAYMENT("Tax/Duty/Fee Payment"),
    LEGAL_CONSULTING_ADVISORY_PAYMENT("Legal/Consulting/Advisory Payment"),
    HEALTHCARE_PAYMENT("Healthcare Payment"),
    CRYPTO_DIGITAL_ASSET_PAYMENT("Crypto/Digital Asset Payment"),
    GAMING_BETTING_PAYMENT("Gaming/Betting Payment"),
    ENTERTAINMENT_SOCIAL_MEDIA_PAYMENT("Entertainment/Social Media Payment");

    private final String value;
}
