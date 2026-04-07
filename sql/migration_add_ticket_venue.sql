ALTER TABLE `ticket_info`
    ADD COLUMN `venue` VARCHAR(128) NULL COMMENT '场馆/地点' AFTER `description`;

UPDATE `ticket_info`
SET `venue` = '待补充场馆'
WHERE `venue` IS NULL OR `venue` = '';
