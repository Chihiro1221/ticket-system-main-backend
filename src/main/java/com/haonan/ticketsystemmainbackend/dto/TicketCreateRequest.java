package com.haonan.ticketsystemmainbackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 创建票据请求
 */
@Data
public class TicketCreateRequest {

    /**
     * 票据ID，需要与库存表使用同一ID进行关联
     */
    @NotBlank(message = "票据ID不能为空")
    private String ticketId;

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
