package com.project.api.domain.coupon;

public record CouponRequestDTO(String code, Integer discount, Long valid) {
}
