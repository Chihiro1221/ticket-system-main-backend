package com.haonan.ticketsystemmainbackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 更新票据请求
 */
@Data
public class TicketUpdateRequest {

    /**
     * 票据名称
     */
    @NotBlank(message = "票据名称不能为空")
    private String name;

    /**
     * 票据描述
     */
    private String description;

    /**
     * 场馆/地点
     */
    @NotBlank(message = "场馆不能为空")
    private String venue;

    /**
     * 票价
     */
    @NotNull(message = "票价不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "票价不能小于0")
    private BigDecimal price;

    /**
     * 票据状态: 0-下架, 1-上架
     */
    @NotNull(message = "票据状态不能为空")
    private Integer status;

    /**
     * 总库存
     */
    @NotNull(message = "总库存不能为空")
    @PositiveOrZero(message = "总库存不能小于0")
    private Integer totalStock;

    /**
     * 场次时间
     */
    private Date eventTime;

    /**
     * 开售时间
     */
    private Date saleStartTime;

    /**
     * 截止售卖时间
     */
    private Date saleEndTime;
}
