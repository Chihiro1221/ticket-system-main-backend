package com.haonan.ticketsystemmainbackend.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;
import lombok.Getter;

/**
 * 库存表
 * @TableName ticket_stock
 */
@TableName(value ="ticket_stock")
@Data
public class TicketStock {
    /**
     * 车次或场次ID
     */
    @TableId
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 当前库存数量
     */
    private Integer stock;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", stock=").append(stock);
        sb.append(", version=").append(version);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}