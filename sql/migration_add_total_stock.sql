ALTER TABLE `ticket_stock`
    ADD COLUMN `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存数量' AFTER `name`;

UPDATE `ticket_stock`
SET `total_stock` = `stock`
WHERE `total_stock` = 0;
