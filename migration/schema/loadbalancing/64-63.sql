use `loadbalancing`;

RENAME TABLE deprecated_ip_version TO ip_version;

ALTER TABLE access_list ADD ip_version varchar(32) DEFAULT NULL;

DROP TABLE IF EXISTS `test`;
CREATE TABLE `test` (
  `k` int(11) NOT NULL,
  `v` int(11) NOT NULL,
  PRIMARY KEY (`k`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

UPDATE `meta` SET `meta_value` = '63' WHERE `meta_key`='version';

