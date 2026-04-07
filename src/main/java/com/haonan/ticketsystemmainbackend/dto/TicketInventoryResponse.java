package com.haonan.ticketsystemmainbackend.dto;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

/**
 * 票务列表/详情聚合响应
 */
@Data
@Builder
public class TicketInventoryResponse {

    private String ticketId;

    private String name;

    private String description;

    private String venue;

    private BigDecimal price;

    private Integer status;

    private Date eventTime;

    private Date saleStartTime;

    private Date saleEndTime;

    private Integer totalStock;

    private Integer remainingStock;

    private Integer soldStock;

    private Integer soldPercentage;

    private Date createTime;

    private Date updateTime;
}
