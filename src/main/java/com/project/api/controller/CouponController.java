package com.project.api.controller;

import com.project.api.domain.coupon.Coupon;
import com.project.api.domain.coupon.CouponRequestDTO;
import com.project.api.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @PostMapping("/event/{eventId}")
    public ResponseEntity<Coupon> addCouponToEvent(@PathVariable("eventId") UUID eventId, @RequestBody CouponRequestDTO data) {
        Coupon coupons = this.couponService.addCouponToEvent(eventId, data);
        return ResponseEntity.ok(coupons);
    }
}
