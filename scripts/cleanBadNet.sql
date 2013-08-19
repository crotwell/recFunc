
delete from hksummary WHERE chanz_id in (select chan_id from netchan WHERE net_id = 10);

delete from hkstack WHERE recfunc_id in (select hkstack.recfunc_id from hkstack JOIN receiverfunction ON (hkstack.recfunc_id = receiverfunction.recfunc_id) JOIN netchan ON (chanz_id = chan_id) WHERE net_id = 10);

delete from receiverfunction WHERE recfunc_id in (select receiverfunction.recfunc_id from receiverfunction JOIN netchan ON (chanz_id = chan_id) WHERE net_id = 10);

delete  FROM channel WHERE chan_id in ( SELECT channel.chan_id FROM network JOIN station ON network.net_id = station.net_id JOIN site ON station.sta_id = site.sta_id JOIN channel ON site.site_id = channel.site_id WHERE network.net_id = 10);

delete  FROM site WHERE site_id in ( SELECT site.site_id FROM network JOIN station ON network.net_id = station.net_id JOIN site ON station.sta_id = site.sta_id  WHERE network.net_id = 10);

delete  FROM station WHERE sta_id in ( SELECT station.sta_id FROM network JOIN station ON network.net_id = station.net_id WHERE network.net_id = 10);

delete FROM network WHERE network.net_id = 10;

