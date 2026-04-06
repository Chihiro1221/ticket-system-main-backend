package com.haonan.ticketsystemmainbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 下单请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 票务ID（车次或场次ID）
     */
    @NotBlank(message = "票务ID不能为空")
    private String ticketId;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Positive(message = "购买数量必须大于0")
    private Integer count;
}
