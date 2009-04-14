from edu.sc.seis.rev.hibernate import RevDB
from edu.sc.seis.sod.hibernate import SodDB, StatefulEventDB, StatefulEvent
from edu.sc.seis.fissuresUtil.hibernate import NetworkDB
from edu.sc.seis.fissuresUtil.display import MicroSecondTimeRange
from edu.sc.seis.fissuresUtil.chooser import ClockUtil
from edu.iris.Fissures.model import UnitImpl, MicroSecondDate, TimeInterval
from edu.iris.Fissures import Time
from edu.sc.seis.fissuresUtil.hibernate import EventDB, NetworkDB
from edu.sc.seis.sod import Status, Stage, Standing
from edu.sc.seis.sod import EventChannelPair
from edu.sc.seis.receiverFunction.hibernate import ReceiverFunctionResult, RFInsertion,  RecFuncDB

soddb = SodDB()
networkdb = NetworkDB()
eventdb = StatefulEventDB()
revdb = RevDB()
rfdb = RecFuncDB()

etName = 'edu.sc.seis.sod.hibernate.StatefulEvent'
ntName = 'edu.iris.Fissures.network.NetworkAttrImpl'
stName = 'edu.iris.Fissures.network.StationImpl'
ctName = 'edu.iris.Fissures.network.ChannelImpl'

__all__ = ['soddb',
	   'revdb',
	   'eventdb',
	   'networkdb',
	   'recentevents',
	   'events',
	   'eventTable',
	   'Status', 'Stage', 'Standing',
	   'eventOnDay',
	   'successfulForEvent',
	   'ecpTable',
	   'formatECP',
	   'formatChannel',
	   'formatStation',
	   'formatEvent',
	   'deleteEvent',
	   'printAllStatus',
	   'ecpStatusSummary',
	   'stationTable',
	   'channelTable',
	   'hql',
	   'etName', 'ntName','stName','ctName', 'checkDuplicates']
	

def hql(query):
    return soddb.getSession().createQuery(query).list()

def printAllStatus():
    for a in Status.ALL:                 
      for s in a:                        
        print '%d %s'%(s.getAsShort(), s)

def recentevents(numEvents=10):
    return revdb.getRecentEvents(numEvents)
    
def eventTable(events):
    for e in events:
	print formatEvent(e)

def formatEvent(e):
    mags = e.preferred.magnitudes
    if hasattr(e, 'status'):
	status = e.status
    else:
	status = ''
    return '%5d %s %4.1fkm %7.2f %7.2f %3.1f %3s  %s %s %s'%(e.dbid, MicroSecondDate(e.preferred.origin_time), e.preferred.my_location.depth.getValue(UnitImpl.KILOMETER), e.preferred.my_location.latitude, e.preferred.my_location.longitude, mags[0].value, mags[0].type, e.preferred.catalog, e.preferred.contributor, status)

def events():
    range = MicroSecondTimeRange(MicroSecondDate(Time('20060116T00:19:26.680Z', -1)), MicroSecondDate(Time('20080719T08:19:26.680Z',-1)))
    events = eventdb.getEventInTimeRange(range)
    eventTable(events)

def eventOnDay(day, standing=Standing.INIT):
    range = MicroSecondTimeRange(MicroSecondDate(Time(day+'T00:00:00.000Z', -1)), MicroSecondDate(Time(day+'T23:59:59.999Z',-1)))
    return eventdb.getEventInTimeRange(range, Status.get(Stage.EVENT_CHANNEL_POPULATION, standing))

def stationTable(stations):
    for s in stations:
	print formatStation(s)

def formatStation(s):
    return '%2s.%5s (%7.2f,%7.2f)'%(s.getNetworkAttr().get_code(),s.get_code(),s.location.latitude, s.location.longitude)

def channelTable(chans):
    for c in chans:
	print formatChannel(c)

def formatChannel(c):
    return '%s %s.%s'%(formatStation(c.site.station), c.site.get_code(), c.get_code())

def ecpTable(ecps):
    for ecp in ecps:
	print formatECP(ecp)

def formatECP(ecp):
    return '%s %s  %s'%(formatChannel(ecp.channel),
		       formatEvent(ecp.event),
		       ecp.status)

def ecpStatusSummary():
    q = soddb.getSession().createQuery('select statusAsShort, count(*) from '+EventChannelPair.getName()+' group by status')
    for line in q.iterate():
        print '%d %s'%(int(line[1]), Status.getFromShort(int(line[0])))

def successfulForEvent(event):
    return soddb.getSuccessful(event)

def reprocessECP(ecpid):
   ecp = soddb.getEventChannelPair(ecpid)
   # check if using event vector
   evp = soddb.getEventVectoPair(ecp)
   if evp == None:
       # straight event-channels
       soddb.retry(LocalSeismogramWaveformWorkUnit(ecp))
   else:
       soddb.retry(MotionVectorWorkUnit(evp))

def updateMag(event, mag, magType):
    event.magnitudes[0] = Magnitude(mag, magType)
    eventdb.getSession().saveOrUpdate(event)

def deleteEvent(event):
    q = eventdb.getSession().createQuery('from '+EventChannelPair.class.getName()+' where event = :event')
    q.setEntity('event', event)
    it = q.iterator()
    for ecp in it:
	eventdb.getSession().delete(ecp)
    eventdb.getSession().delete(event)
    eventdb.commit()

def checkDuplicates(netCode, staCode):
    l = hql('select A from '+ReceiverFunctionResult.getName()+' as A ,  '+ReceiverFunctionResult.getName()+'  as B where A.channelGroup.channel1.site.station.networkAttr.id.network_code = B.channelGroup.channel1.site.station.networkAttr.id.network_code and A.channelGroup.channel1.id.station_code = B.channelGroup.channel1.id.station_code and A.event = B.event and A.dbid < B.dbid and A.channelGroup.channel1.id.station_code = \'%s\' and A.channelGroup.channel1.site.station.networkAttr.id.network_code = \'%s\''%(staCode, netCode))
    print 'found %d duplicates for %s.%s'%(len(l), netCode, staCode)
    if len(l) == 0:
	soddb.commit()
	return
    for r in l:
	s = rfdb.getSumStack(r.channelGroup.channel1.site.station.networkAttr, r.channelGroup.channel1.id.station_code, 2.5)
	s.getIndividuals().remove(r)
	soddb.getSession().saveOrUpdate(s)
	soddb.getSession().delete(r)
    insertion = rfdb.getInsertion(r.channelGroup.channel1.site.station.networkAttr, r.channelGroup.channel1.id.station_code, 2.5)
    if insertion == None:
	insertion = RFInsertion(r.channelGroup.channel1.site.station.networkAttr, r.channelGroup.channel1.id.station_code, 2.5)
    else:
	insertion.setInsertTime(ClockUtil.now().getTimestamp())
    soddb.getSession().saveOrUpdate(insertion)
    soddb.commit()
