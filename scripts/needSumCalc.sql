SELECT DISTINCT network.net_code, station.sta_code FROM network 
    JOIN station ON (network.net_id = station.net_id ) 
    JOIN location AS loc ON (station.loc_id = loc.loc_id) 
    JOIN site ON (station.sta_id = site.sta_id ) 
    JOIN channel ON (channel.site_id = site.site_id ) 
    JOIN  receiverFunction ON (chanz_id = chan_id ) 
    LEFT JOIN recfuncQC ON (receiverFunction.recfunc_id = recfuncQC.recfunc_id )
    JOIN origin ON (receiverfunction.origin_id = origin.origin_id) 
    JOIN hkstack ON (hkstack.recfunc_id = receiverfunction.recfunc_id)
    JOIN hksummary ON (network.net_id = hksummary.net_id AND station.sta_code = hksummary.sta_code)
    WHERE hksummary.inserttime < hkstack.inserttime 
    AND loc.loc_lon > -126 AND loc.loc_lon < -114 
    AND loc.loc_lat > 30 AND loc.loc_lat < 50 
    AND itr_match >= 80 
    AND (keep = true OR keep IS NULL);
