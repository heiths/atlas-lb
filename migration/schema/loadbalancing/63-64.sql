use `loadbalancing`;

RENAME TABLE ip_version TO deprecated_ip_version;
RENAME TABLE host_backup TO deprecated_host_backup;

ALTER TABLE access_list DROP COLUMN ip_version;

DROP TABLE IF EXISTS `test`;

UPDATE `meta` SET `meta_value` = '64' WHERE `meta_key`='version';

