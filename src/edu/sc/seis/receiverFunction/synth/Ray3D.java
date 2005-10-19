package edu.sc.seis.receiverFunction.synth;


public class Ray3D {

    public Ray3D() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    /*

      program ray3d
c
c    calculates travel times, azimuthal anomalies, ray parameter
c      anomalies for primary and multiple converted waves
c      in a dipping structure, implementation of the method of
c      Langston (1977; bssa)
c
c    Written by T.J. Owens, march 1982, revised innumerable times
c                                       since then
c    Version A-1; Revised May 1987
c                 Revised Oct 1989 for round-off problems around
c                   Loop 32 - mod by tjo
c
      dimension strike(100),dip(100),z(100),alpha(100),beta(100),
     *          rho(100),eta(3,100),q(3,5000),q0(3),v(2,100),qv(5000),
     *          dist(3),qloc1(3),qloc2(3),a(3,3),iface(5000),layer(100),
     *          mulyr(100),amag(3,5000),hmag(3,5000),raymag(3,5000),
     *          rayhil(3,5000),exmuls(100),raytim(5000),spike(3600,3),
     *          synth(8200),direct(3),hilbt(3600,3),synhil(8200)
      logical yes,yesno,pors,again,amps,ppps(100),free,instrm,qcorr,
     *        mormul(100),amps1,pps2,mormu2
      integer trans,refl,type,ior(3),blank,exmuls,ippps(100),ounit
      integer lll
      character struc*32,synout*32,title*32,comp(3)*4,spn(3)*6,syn(3)*6,
     *          name*32
c **********************************************************************
c
c common block info for link with subroutine sacio
c
      real instr
      integer year,jday,hour,min,isec,msec
      character*8 sta,cmpnm,evnm
      common /tjocm/ dmin,dmax,dmean,year,jday,hour,min,isec,msec,sta,
     *         cmpnm,caz,cinc,evnm,baz,delta,p0,depth,decon,agauss,
     *              c,tq,instr,dlen,begin,t0,t1,t2
c
c **************************************************************************
c
c   parameter definitions may be found sacio comments
c
      common /cord/ a
      common /amcal/ qloc1,qloc2,vb,va,sinib,sinia,vp1,vs1,rho1,
     *               vp2,vs2,rho2,free,type
      common /transm/ q,qv,v,alpha,beta,rho,strike,dip,iface,jhilb,
     *                amag,hmag,layer,amps,trans,refl,nlyrs
      common /raywrt/ eta,z,raymag,rayhil,raytim,ntim,p0r,pors,
     *                oldlyr,q0,direct,tdirec,baz1
      common /ar5/ exmuls,ppps,mormul,ippps
      common /ar4/ synhil
      common /ar3/ hilbt
      common /ar2/ synth
      common /ar1/ spike
      common /innout/ inunit,ounit
c
c  ray3d generates specific output file names for synthetics
c    spike series are named "spike name"_sp.[zrt]
c    if synthetic is convolved with a source function,
c    the synthetic is "synthetic name"_sy.[zrt]
c    where "spike name" and "synthetic name" are requested by
c    the program.
c
      data comp/'vert','rad ','tang'/,ior/3,1,2/,
     *     spn/'_sp.z ','_sp.r ','_sp.t '/,
     *     syn/'_sy.z ','_sy.r ','_sy.t '/
      rad(deg)=deg/57.2957795
      inunit=5
      ounit=6
c
c  signal is a RIDGE routine used to request that
c  floating point underflows be ignored, this will
c  probably be deleted on most machines
c
      lll = signal(119,dun,1)
      call iniocm
      write(ounit,120)
c
c   all output is to file ray3d.out
c
      open(unit=9,file='ray3d.out',form='formatted')
      rewind 9
      write(9,120)
      again=.false.
  120 format(' ray tracer for dipping structures',/)
      call asktxt('Specify structure file: ',struc)
      call rdlyrs(struc,nlyrs,title,alpha,beta,rho,z,
     *            dum1,dum2,strike,dip,-1,ier)
c
c     adjust input values from rdlyrs to necessary form
c
      tmpz1=z(1)
      z(1)=0.
      tmps1=strike(1)
      strike(1)=0.
      tmpd1=dip(1)
      dip(1)=0.
      do 48 i48=2,nlyrs
      tmps2=strike(i48)
      tmpd2=dip(i48)
      strike(i48)=tmps1
      dip(i48)=tmpd1
      tmps1=tmps2
      tmpd1=tmpd2
      tmpz2=z(i48)
      z(i48)=z(i48-1)+tmpz1
      tmpz1=tmpz2
   48 continue
      write(9,778) struc,title,nlyrs
      do 49 i=1,nlyrs
         write(9,779) i,alpha(i),beta(i),rho(i),strike(i),dip(i),z(i)
   49 continue
      write(9,780)
  778 format(' structure file: ',a10,' model ',a10,1x,i2,' layers',/,
     *       ' layer     vp    vs     dens     strike     dip     z')
  779 format(3x,i2,4x,f4.2,3x,f4.2,5x,f4.2,5x,f6.2,4x,f4.1,4x,f5.1)
  780 format(1x,/)
c
c     ask all initial questions
c
    6 p0=ask('Specify ray param. for incident wave: ')
      p0r=p0
      baz=ask('Back azimuth of incident ray: ')
      baz1=baz
      pors=yesno('P-wave (y or n) ? ')
      if(pors) go to 16
         amps=.false.
         go to 15
   16 amps=yesno('Calculate any amplitudes (y or n) ? ')
      if(.not.amps) go to 15
         pps2=yesno('Pp and Ps only (y or n) ? ')
         pamp=ask('Incident p amplitude = ')
   15 sini=p0*alpha(nlyrs)
      if(.not.pors) sini=p0*beta(nlyrs)
      numint = nlyrs -1
      do 22 i22=1,numint
         layer(i22)=i22+1
         mulyr(i22)=0
         ppps(layer(i22))=.false.
         if(pps2) ppps(layer(i22))=.true.
         mormul(layer(i22))=.false.
   22 continue
   64 write(ounit,107)
  107 format(' your layer ray tracing parameters are: ',//,
     *       'interface  ppps  mormul ')
      do 21 i21=1,numint
        write(ounit,105) layer(i21),ppps(layer(i21)),mormul(layer(i21))
   21 continue
  105 format(5x,i3,5x,l1,5x,l1)
      if(yesno('OK ? (y or n) ')) go to 18
      write(ounit,102)
  102 format(' enter the # of interfaces to trace from (i2)')
      read(inunit,103) numin2
      if(numin2.le.0) go to 60
      numint=numin2
      write(ounit,101)
  101 format(' enter the interface numbers (40i2)')
      read(inunit,103) (layer(i),i=1,numint)
  103 format(40i2)
   60 if(.not.yesno('Change PpPs options (y or n) ? ')) go to 70
         write(ounit,108)
  108 format('enter interface #s which need ppps changed from current',
     *       ' value (40i2) ')
         read(inunit,103) (ippps(i),i=1,40)
         do 71 i71=1,numint
            if(ippps(i71).eq.0) go to 70
            if(ppps(ippps(i71))) then
              ppps(ippps(i71))=.false.
            else
              ppps(ippps(i71))=.true.
            endif
   71 continue
   70 mormu2=yesno('Calculate extra multiples ? (y or n) ')
      if(.not.mormu2) go to 69
      write(ounit,104)
  104 format(' enter interface numbers for extra multiple',
     *       '  calculations (40i2)')
      read(inunit,103) (mulyr(i),i=1,30)
      do 20 i20=1,100
         if(mulyr(i20).ne.0) go to 20
         nmults=i20-1
         go to 61
   20 continue
   61 if(nmults.eq.0) go to 60
      if(yesno('Calculate extra mults for all rays ? ')) go to 62
         write(ounit,106)
  106 format(' enter only interfaces which have rays that need',
     *       ' extra mults tacked on')
      read(inunit,103) (exmuls(i),i=1,40)
      do 72 i72=1,nlyrs
   72 mormul(i72)=.false.
      do 63 i63=1,40
         if(exmuls(i63).eq.0) go to 64
         mormul(exmuls(i63))=.true.
   63 continue
   69 go to 64
   62 do 65 i65=1,numint
   65    mormul(layer(i65))=.true.
      go to 64
   18 nrays=1
      do 181 i181=1,numint
         nr2=9
         if(ppps(layer(i181))) nr2 = 1
         if(mormul(layer(i181))) nr2 = nr2 + nr2*4*nmults
         nrays=nrays + nr2
  181 continue
      if(nrays.le.5000) go to 182
        write(ounit,183) nrays
  183   format(' nrays = ',i5,' is too big - try again ')
        go to 64
  182 if(again) go to 14
c
c   calculate layer interface unit normal vectors in global coordinates
c
      do 1 i1=1,nlyrs
         strike(i1)=rad(strike(i1))
         dip(i1)=rad(dip(i1))
         call norvec(strike(i1),dip(i1),eta(1,i1))
    1 continue
c
c   define incident ray unit vector in global coordinates
c
   14 q0(1)=-sini*cos(rad(baz))
      q0(2)=-sini*sin(rad(baz))
      q0(3)=-sqrt(1. - sini*sini)
c
c   set up velocity arrays and other initital conditions
c
      do 2 i2=1,nlyrs
         if(.not.pors) go to 3
            v(1,i2)=alpha(i2)
            v(2,i2)=beta(i2)
            go to 2
    3    v(1,i2)=beta(i2)
         v(2,i2)=alpha(i2)
    2 continue
      trans=1
      refl=-1
      qv(1)=v(1,nlyrs)
      iface(1)=0
      do 17 i17=1,3
      call zeerow(amag(i17,1),1,5000)
      call zeerow(amag(i17,1),1,5000)
      q(i17,1)=q0(i17)
      if(.not.amps) go to 17
      amag(i17,1)=pamp*q0(i17)
      hmag(i17,1)=0.
      if(i17.lt.3) go to 17
         vp1=alpha(nlyrs)
         vs1=beta(nlyrs)
         rho1=rho(nlyrs)
         free=.false.
         ntim=1
   17 continue
c
c   s t a r t   r a y   t r a c i n g   s e c t i o n
c
c   find ray unit vectors for the direct ray
c
      ihilb=0
      jhilb=0
      iq=1
      call trnsmt(1,nlyrs,iq,1,.true.)
c
c   if iq= -999 then a head wave has been generated and the run will bomb
c
      if(iq.eq.-999) then
         write(ounit,133)
  133    format(' Immediate problems with head waves ',
     *          'on direct wave pass - Check velocity model !!')
         stop
      endif
      nlr=nlyrs
      call rayfin(nlr,1,0,0,0,.true.,.false.)
      amps1=amps
c
c   calculate the other rays, first all the unconverted rays & their
c     multiples, then the converted waves & their multiples
c     loops 50,52, & 53 do extra multiples, if necessary
c
      do 4 i4=1,2
         do 8 i8=1,numint
            amps=amps1
c
c           if doing the converted waves
c               recalculate the necessary q-vectors
c
            if(i4.eq.1) go to 13
               iq=nlyrs - layer(i8) + 1
               if(.not.amps) go to 28
               vp1=alpha(nlyrs-iq+1)
               vs1=beta(nlyrs-iq+1)
               rho1=rho(nlyrs-iq+1)
   28       loopst=iq
            call trnsmt(loopst,nlyrs,iq,i4,.true.)
c
c   if iq = -999, then problem phases exist -- this and all subsequent rays
c                      are skipped
c
            if(iq.eq.-999) go to 8
c
c           print results for direct converted waves
c
            call rayfin(nlr,i4,0,0,layer(i8),.false.,.false.)
                  if(.not.mormul(layer(i8))) go to 13
                  iqmul=iq
                  do 66 i66=1,nmults
                     if(mulyr(i66).eq.layer(i8).and.
     *                 (.not.ppps(layer(i8)))) go to 66
                     iqi=iqmul
                     do 67 i67=1,2
                        vs1=beta(1)
                        vp1=alpha(1)
                        rho1=rho(1)
                        rho2=0.0
                        vp2=0.
                        vs2=0.
                        call raydwn(iqi,i67,mulyr(i66),iq)
c
c   if iq = -999, then problem phases exist -- this and all subsequent rays
c                      are skipped
c
                        if(iq.eq.-999) go to 66
                        miqdwn=iq
                        do 68 i68=1,2
                           call rayup(miqdwn,i68,mulyr(i66),iq)
c
c   if iq = -999, then problem phases exist -- this and all subsequent rays
c                      are skipped
c
                           if(iq.eq.-999) go to 68
                           call rayfin(iq,0,i67,i68,mulyr(i66),
     *                                 .false.,.true.)
   68                   continue
   67                continue
   66             continue
   13       if(ppps(layer(i8))) amps=.false.
            do 10 i10=1,2
               vs1=beta(1)
               vp1=alpha(1)
               rho1=rho(1)
               rho2=0.0
               vp2=0.
               vs2=0.
               call raydwn(nlyrs,i10,layer(i8),iq)
c
c   if iq = -999, then problem phases exist -- this and all subsequent rays
c                      are skipped
c
               if(iq.eq.-999) go to 10
               iqdown=iq
               do 11 i11=1,2
                  call rayup(iqdown,i11,layer(i8),iq)
c
c   if iq = -999, then problem phases exist -- this and all subsequent rays
c                      are skipped
c
                  if(iq.eq.-999) go to 11
                  call rayfin(iq,i4,i10,i11,layer(i8),.false.,.true.)
                  if(.not.mormul(layer(i8))) go to 11
                  iqmul=iq
                  do 50 i50=1,nmults
                     iqi=iqmul
                     do 52 i52=1,2
                        vs1=beta(1)
                        vp1=alpha(1)
                        rho1=rho(1)
                        rho2=0.0
                        vp2=0.
                        vs2=0.
                        call raydwn(iqi,i52,mulyr(i50),iq)
c
c   if iq = -999, then problem phases exist -- this and all subsequent rays
c                      are skipped
c
                        if(iq.eq.-999) go to 52
                        miqdwn=iq
                        do 53 i53=1,2
                           call rayup(miqdwn,i53,mulyr(i50),iq)
c
c   if iq = -999, then problem phases exist -- this and all subsequent rays
c                      are skipped
c
                           if(iq.eq.-999) go to 53
                           call rayfin(iq,0,i52,i53,mulyr(i50),
     *                                 .false.,.true.)
   53                   continue
   52                continue
   50             continue
   11          continue
   10       continue
    8    continue
    4 continue
      amps=amps1
      if(.not.amps) go to 29
      ntim=ntim-1
c
c  ray3d.amps can be a big file if many rays are traced
c     use with caution
c
      yes=yesno('Create ray3d.amps ? ')
      if(.not.yes) go to 180
      open(unit=8,file='ray3d.amps',form='formatted')
      write(8,788) struc,title,nlyrs,p0r,baz
  788 format(' file: ',a10,' model ',a10,1x,i2,' layers ',
     *       ' ray parameter ',f7.5,' back az. ',f6.2)
  180 do 27 i27=1,ntim
         call rtoi(raymag(1,i27),cos(rad(baz)),sin(rad(baz)),-1.,
     *             .false.)
         call rtoi(rayhil(1,i27),cos(rad(baz)),sin(rad(baz)),-1.,
     *             .false.)
         rayhil(3,i27)=-rayhil(3,i27)
         raymag(3,i27)=-raymag(3,i27)
         if(yes) write(8,122) i27,(raymag(j,i27),j=1,3),
     *                 (rayhil(j,i27),j=1,3),raytim(i27)
   27 continue
  122 format(1x,i3,1x,7e15.7)
      if(jhilb.eq.1) write(ounit,781)
  781 format(' phase shifted arrivals exist ')
      yes=yesno('Save this spike ? ')
      if(.not.yes) go to 29
      dt=ask('Sampling rate (sec): ')
      dura=ask('Signal duration (secs): ')
      delay=ask('First arrival delay: ')
      npts=ifix(dura/dt + .5) + 1.
      begin = 0.
      do 30 i30=1,3
         call zeerow(spike(1,i30),1,3600)
         if(jhilb.eq.1) call zeerow(hilbt(1,i30),1,npts)
   30 continue
c
c Section below modified on 10/12/89 on Sun-4 to avoid roundoff
c problems identified by John Cassidy at UBC and known to occur
c on Sun-3 versions of ray3d
c Modifications taken from ray3d subroutine in timinv.f
c
      dtby2 = dt/2.
      do 32 j32=1,ntim
         itinc=0
         rtpdel=raytim(j32) + delay
         isampi=rtpdel/dt
         raytm=dt*float(isampi) +dtby2
         if(raytm.lt.rtpdel) itinc=1
         irayl=isampi + itinc + 1
         do 33 i33=1,3
            spike(irayl,i33)=spike(irayl,i33) + raymag(i33,j32)
            if(jhilb.eq.0) go to 33
               hilbt(irayl,i33)=hilbt(irayl,i33) + rayhil(i33,j32)
   33       continue
c
c  END of 10/12/89 modifications
c
   32    continue
      sta=struc(1:8)
      year=1983
      jday=1
      hour=0
      min=0
      isec=0
      msec=0
      call asktxt('Spike output file: ',synout)
      iblank=blank(synout)
      if(iblank.lt.2) go to 35
      call asktxt('Spike name: ',name)
      evnm=name(1:8)
      do 34 i34=1,3
         cmpnm=comp(i34)
         goto (40,41,42) i34
   40    cinc=0.
         caz=0.
         go to 43
   41    caz=baz+180.
         cinc=90.
         go to 43
   42    caz=baz+270.
         cinc=90.
   43    if(caz.gt.360.) caz=caz-360.
         synout(1:iblank+6)=synout(1:iblank)//spn(i34)
         call minmax(spike(1,ior(i34)),npts,dmin,dmax,dmean)
         call sacio(synout,spike(1,ior(i34)),npts,dt,-1)
   34 continue
   35 yes=yesno('Convolve w/ source function ? ')
      if(.not.yes) go to 29
      instrm=yesno('Include 15-100 instrm response ? ')
      qcorr=yesno('Include futterman q ? ')
      if(qcorr) tq=ask('t/q = ')
      nft=npowr2(npts)
      call asktxt('Synthetic output file: ',synout)
      call asktxt('Synth name: ',name)
      kst=0
      evnm=name(1:8)
      sta=struc(1:8)
      if(instrm) instr=1.
      iblank=blank(synout)
      ist=-1
      do 36 i36=1,3
         call zeerow(synth,1,8200)
         if(jhilb.eq.1) call zeerow(synhil,1,8200)
         do 37 i37=1,npts
            if(jhilb.eq.1) synhil(i37)=hilbt(i37,ior(i36))
   37       synth(i37)=spike(i37,ior(i36))
         call mkseis(synth,synhil,instrm,qcorr,tq,nft,dt,kst,jhilb)
         cmpnm=comp(i36)
         goto (44,45,46) i36
   44    cinc=0.
         caz=0.
         go to 47
   45    caz=baz+180.
         cinc=90.
         go to 47
   46    caz=baz+270.
         cinc=90.
   47    if(caz.gt.360.) caz=caz-360.
         synout(1:iblank+6)=synout(1:iblank)//syn(i36)
         call minmax(synth,npts,dmin,dmax,dmean)
         call sacio(synout,synth,npts,dt,-1)
   36 continue
      go to 35
   29 again=yesno('Trace another in the same model ? (y or n) ')
      if(again) go to 6
      close(unit=9)
      if(amps) close(unit=8)
      stop
      end
      
      */
    
    /*
      subroutine anom(q,v,az,p,sini)
c
c   calculates the azimuth and ray parameter of a ray defined by q
c     in a medium of velocity v, assuming the surface is horizontal
c
      dimension q(1)
      deg(rad)=rad*57.2957795
      cosi=-q(3)
      sini=sqrt(1.-cosi*cosi)
      p=sini/v
c
c as always vertical incidence case is special
c
      if(sini.gt..0001) then
         sinb=-q(2)/sini
         cosb=-q(1)/sini
         az=atan2(sinb,cosb)
         az=deg(az)
      else
         az=0.0
      endif
  101 format(1x,5e15.7)
      return
      end
      */
    public static void anom() {
        
    }
      /*
      function timcor(x1,x2,q0,v)
c
c  finds the time diference between a ray which enters the
c   layering at point x2 to one which enters the layering at
c   x1 if the half space unit ray vector is q0 and the half
c   space velocity is v
c
      dimension x1(1),x2(1),q0(1),r(3)
      do 1 i=1,3
   1  r(i)=x2(i)-x1(i)
      corr=dot(r,q0)
      timcor=corr/v
      return
      end
      */
    public static void timecor() {
        
    }
    /*
      subroutine norvec(strike,dip,eta)
c
c  calculates the interface unit normal vector, given the layer
c    strike and dip in radians
c
      dimension eta(3)
      sins=sin(strike)
      coss=cos(strike)
      sind=sin(dip)
      cosd=cos(dip)
      eta(1)=sind*sins
      eta(2)=-sind*coss
      eta(3)=cosd
      return
      end
      */
    public static void norvec() {
        
    }
    /*
      subroutine timdis(dist,q,ii,jj,vel,n,time,
     *                  iface,eta,kk,ll,z,lnumbr,dislyr,deplyr)
c
c   calculates the point a ray, specified by the n ray unit normals
c     given in q, enters the layered medium and its travel-time in
c     the layered system
c
      dimension q(ii,jj),vel(n),iface(n),eta(kk,ll),z(1),dist(1)
      time=0.
c
c   calculates time & dist for the nth to 2nd q-vectors since vector
c     #1 is the incident ray
c
      do 1 i1=1,n-1
         j1=n - i1 + 1
         unum=eta(3,iface(j1))*(z(iface(j1))-dist(3))
     *       -eta(2,iface(j1))*dist(2)
     *       -eta(1,iface(j1))*dist(1)
         u=unum/dot(eta(1,iface(j1)),q(1,j1))
         do 2 i2=1,3
    2       dist(i2)=dist(i2) + u*q(i2,j1)
         time=abs(u)/vel(j1)  + time
         if(iface(j1).eq.lnumbr) then
            dislyr=sqrt(dist(1)**2 + dist(2)**2)
            deplyr=dist(3)
         endif
    1 continue
      if(lnumbr.eq.0) then
          deplyr=dist(3)
          dislyr=sqrt(dist(1)**2 + dist(2)**2)
      endif
      return
      end
      */
    public static void timedis() {
        
    }
    /*
      subroutine snell(qb,vb,qa,va,itype,sinib,sinia)
c
c   calculates the ray unit normal vector, qa resulting from an
c     incident unit normal vector, qb interacting with a velocity
c     interface.  the medium velocity of qb is vb, the medium
c     velocity of qa is va
c
      dimension qb(1),qa(1)
      integer ounit
      common /innout/ inunit,ounit
      torr=float(itype)
      sinib=sqrt(1.-qb(3)*qb(3))
c
c   check for near-vertical incidence.  If sinib < 0.002, then ray is
c     set to true vertical incidence, to avoid instabilities in the
c     calculation of the factor "a" below.  This corresponds to angles
c     of incidence of less than 0.11 degrees, so this manipulation should
c     not cause any significant errors
c***********************************
      if(sinib.le..002) then
         sinib=0.
         qb(3)=abs(qb(3))/qb(3)
         qb(2)=0.
         qb(1)=0.
         sinia=0.
         qa(1)=qb(1)
         qa(2)=qb(2)
         qa(3)=torr*qb(3)
         return
      endif
c************************************
c  process all other rays 
c
      sinia=va*sinib/vb
c
c   check for problems with head waves +/or post critically reflected converted
c             phases
c    if any exist, flag the ray and return
c
      if(sinia.ge.1.00) then
         write(ounit,100) vb,va,qb(3)
         if(torr.lt.0..and.qb(3).lt.0.) write(ounit,101)
         if(torr.lt.0..and.qb(3).ge.0.) write(ounit,102)
         if(torr.gt.0.) write(ounit,103)
         itype = -999
         return
      endif
      if(sinia.lt..0001) then
        a=0.
      else
         a=sinia/sqrt(qb(1)*qb(1) + qb(2)*qb(2))
         qa(1)=a*qb(1)
         qa(2)=a*qb(2)
         qa(3)=torr*(qb(3)/abs(qb(3)))*sqrt(1. - sinia*sinia)
      endif
      return
  100 format(' For vb => va of',f6.3,' => ',f6.3,' and qb = ',f6.4)
  101 format('    ===>  A free surface s-to-p reflection is critical ')
  102 format('    ===>  An internal s-to-p reflection is critical ')
  103 format('    ===>  A head wave has been generated')
      end
      */
    public static void snell() {
        
    }
    /*
      subroutine wrtray(lyr,az,p,time,baz,p0,pors,init,i4,i10,i11
     *                  ,oldlyr,sini,tdirec)
c
c  writes the results of a ray tracing loop into unit 10
c
      dimension type(2),wave(4,2),prim(2,2)
      logical pors,emult
      character type*1,prim*2,wave*3
      data type/'p','s'/,prim/'pp','ss','ps','sp'/,
     *     wave/'pmp','pms','smp','sms','sms','smp','pms','pmp'/
      angle=asin(sini)
      angle=angle*57.2957795
      emult=.false.
      if(i4.ne.0) go to 8
         emult=.true.
         go to 1
    8 if(init.ne.0) go to 1
         iprim=1
         if(.not.pors) iprim=2
         if(i4.ne.1) go to 6
            itype=1
            if(.not.pors) itype=2
            write(9,100) type(itype),baz,p0,tdirec
            t1=0.
            write(9,102)
            write(9,101) prim(iprim,i4),t1,az,p,angle
            oldlyr=0
            return
    6    write(9,103) lyr
         write(9,102)
         write(9,104) prim(iprim,i4),lyr,time,az,p,angle
         oldlyr=lyr
         return
    1 ip=1
      if(.not.pors) ip=2
      if(i10.ne.1) goto 2
         if(i11.eq.1) go to 3
            iwave=2
            go to 5
    3       iwave=1
            go to 5
    2 if(i11.eq.1) go to 4
         iwave=4
         go to 5
    4    iwave=3
    5 if(emult) go to 9
      if(lyr.eq.oldlyr) go to 7
        write(9,103) lyr
         write(9,102)
         oldlyr=lyr
    7 write(9,105) prim(ip,i4),wave(iwave,ip),lyr,time,az,p,angle
      return
    9 if(iwave.eq.1.and.ip.eq.1) write(9,106) lyr
      write(9,107) wave(iwave,ip),time,az,p,angle
      return
  100 format(///' incident ',a1,'-wave, back azimuth: ',f6.2,
     *       ' ray parameter: ',f7.4,/,' direct arrival spends ',f7.3,
     *       ' secs in layering',/,' all times relative to direct ray'
     *       ,/)
  101 format(5x,a2,5x,'direct',2x,f7.3,3x,f7.2,7x,f7.4,6x,f5.2)
  102 format(' wave type   layer    time     azimuth     ray param.',
     *       '   angle')
  103 format(1x,/,' layer ',i2)
  104 format(5x,a2,7x,i2,4x,f7.3,3x,f7.2,7x,f7.4,6x,f5.2)
  105 format(3x,a2,a3,6x,i2,4x,f7.3,3x,f7.2,7x,f7.4,6x,f5.2)
  106 format(63x,'extra multiples from layer ',i2,/,
     *       63x,' type    time        az.          p         angle')
  107 format(64x,a3,3x,f7.3,4x,f7.2,6x,f7.4,6x,f5.2)
      end
      */
    public static void wrtray() {
        
    }
    /*
      subroutine ampcal(amagb,hmagb,amaga,hmaga,strike,dip,ihilb)
c
c subroutine to calculate amplitudes for rays from ray3d
c
c   i n p u t
c
c
      dimension qb(3),qa(3),amagb(1),amaga(1),r3(3),rt(3),at(3),ai(3),
     *          a(3,3),hmaga(1),hmagb(1),rth(3),ht(3)
      logical free
      integer type,ounit
      common /cord/ a
      common /amcal/ qb,qa,vb,va,sinib,sinia,vp1,vs1,rho1,
     *               vp2,vs2,rho2,free,type
      common /innout/ inunit,ounit
      call zeerow(r3,1,3)
      call zeerow(rt,1,3)
      call zeerow(at,1,3)
      call zeerow(ai,1,3)
      call zeerow(ht,1,3)
      call zeerow(rth,1,3)
      rshph=0.
      rph=0.
      rphx=0.
      rphy=0.
      rphz=0.
      rmag=0.
      ncode=0
      eps=.0001
      ihilb=0
      rshmag=0.
      pi=3.14159
c
c vertical incidence case requires special treatment
c
      if(sinib.gt.eps) then
         cosphi=-qb(1)/sinib
         sinphi=-qb(2)/sinib
      else
         cosphi=-1.
         sinphi=0.
      endif
      nd=0
      if(abs(qb(3))/qb(3).gt.0) nd=1
      p=sinib/vb
      if(free) go to 10
      ro2=rho2
c
c   find ncode for non-free surface case
c
      if(abs(vb-vp1).gt.eps) go to 1
        call rcomp(ai,1,nd,sinib,.true.)
        if(type.lt.0) go to 2
        if(abs(va-vp2).lt.eps) ncode=3
        if(abs(va-vs2).lt.eps) ncode=4
        go to 3
    2   if(abs(va-vp1).lt.eps) ncode=1
        if(abs(va-vs1).lt.eps) ncode=2
        go to 3
    1 if(abs(vb-vs1).gt.eps) go to 4
        call rcomp(ai,2,nd,sinib,.true.)
        if(type.lt.0) go to 5
        if(abs(va-vs2).lt.eps) ncode=8
        if(abs(va-vp2).lt.eps) ncode=7
        go to 3
    5   if(abs(va-vp1).lt.eps) ncode=5
        if(abs(va-vs1).lt.eps) ncode=6
    3 ncase=0
      if(ncode.eq.0) go to 4
      if(ncode.le.4) go to 7
         ncase=4
         go to 7
c
c  find ncode for free surface case
c
   10 ro2=0.0
      vp2=0.
      vs2=0.
      if(type.eq.0) go to 15
      if(abs(vb-vp1).gt.eps) go to 12
         call rcomp(ai,1,nd,sinib,.true.)
         if(abs(va-vs1).lt.eps) ncode=2
         if(abs(va-vp1).lt.eps) ncode=1
         go to 13
   12    if(abs(vb-vs1).gt.eps) go to 4
         call rcomp(ai,2,nd,sinib,.true.)
         if(abs(va-vs1).lt.eps) ncode=4
         if(abs(va-vp1).lt.eps) ncode=3
   13 ncase=0
      if(ncode.eq.0) go to 4
      if(ncode.le.2) go to 7
         ncase=2
         go to 7
c
c
c  f i n d  f r e e  s u r f a c e  e f f e c t
c
c
   15 if(abs(vb-vp1).lt.eps) go to 16
      if(abs(vb-vs1).lt.eps) go to 17
      go to 4
   16 call rcomp(ai,1,nd,sinib,.true.)
      call coef8(p,vp1,vs1,rho1,vp2,vs2,0.0,5,nd,rx,rphx)
      call coef8(p,vp1,vs1,rho1,vp2,vs2,0.0,6,nd,rz,rphz)
      ry=0.
      rphy=0.
      go to 18
   17 call rcomp(ai,2,nd,sinib,.true.)
      call coef8(p,vp1,vs1,rho1,vp2,vs2,0.0,7,nd,rx,rphx)
      call coef8(p,vp1,vs1,rho1,vp2,vs2,0.0,8,nd,rz,rphz)
      call coefsh(p,vs1,rho1,vs2,0.0,2,ry,rphy)
   18 if(abs(rphx+pi).gt.eps) go to 22
        rphx=0.
        rx=-rx
   22 if(abs(rphy+pi).gt.eps) go to 23
        rphy=0.
        ry=-ry
   23 if(abs(rphz+pi).gt.eps) go to 24
        rphz=0.
        rz=-rz
   24 do 19 i19=1,3
         rth(i19)=hmagb(i19)
   19    rt(i19)=amagb(i19)
c
c  rt is in global coordinates, but this is equivalent to interface
c    coordinates for the free surface. so transform rt directly to
c    the ray coordinate system
c
      call rtoi(rt,cosphi,sinphi,qb(3),.false.)
      call rtoi(rth,cosphi,sinphi,qb(3),.false.)
      phck=0.
      phck=abs(rphz)+abs(rphx)+abs(rphy)
      if(phck.gt.eps) ihilb=1
      dotar=dot(ai,rt)
c
c vertical incidence can sometimes blow up this step
c    check first
c
      if(abs(dotar).lt.eps) go to 56
      dotar=abs(dotar)/dotar
   56 doth=dot(ai,rth)
      if(abs(doth).lt.eps) go to 26
      doth=abs(doth)/doth
   26 amh=sqrt(rth(1)*rth(1) + rth(3)*rth(3))*doth
      amb=sqrt(rt(1)*rt(1) + rt(3)*rt(3))*dotar
      amaga(1)=rx*(amb*cos(rphx) - amh*sin(rphx))
      amaga(2)=ry*(rt(2)*cos(rphy) - rth(2)*sin(rphy))
      amaga(3)=rz*(amb*cos(rphz) - amh*sin(rphz))
      hmaga(1)=rx*(amh*cos(rphx) + amb*sin(rphx))
      hmaga(2)=ry*(rth(2)*cos(rphy) + rt(2)*sin(rphy))
      hmaga(3)=rz*(amh*cos(rphz) + amb*sin(rphz))
      call rtoi(amaga,cosphi,sinphi,qb(3),.true.)
      call rtoi(hmaga,cosphi,sinphi,qb(3),.true.)
      return
c
c
c  g e n e r a l  c o e f i c i e n t  c a l c u l a t i o n
c
c  first find rt, the incident displacement vector in ray coordinates
c        &    rth, the distorted displacement vector in ray coordinates
c
    7 call coord(amagb,strike,dip,rt,'local',.true.)
      call coord(hmagb,strike,dip,rth,'local',.true.)
      call rtoi(rt,cosphi,sinphi,qb(3),.false.)
      call rtoi(rth,cosphi,sinphi,qb(3),.false.)
      call coef8(p,vp1,vs1,rho1,vp2,vs2,ro2,ncode,nd,rmag,rph)
      call rcomp(r3,ncode-ncase,nd,sinia,.false.)
      if(abs(rph + pi).gt.eps) go to 20
         rph=0.
         rmag=-rmag
   20 at(2)=0.0
c
c  if incident & resulting waves are both s-waves, find sh coeficient
c
      if(ncode.le.4) go to 9
      if(ncode.eq.6) then
          ncodsh=1
      elseif(ncode.eq.8) then
          ncodsh=2
      else
          go to 9
      endif
      call coefsh(p,vs1,rho1,vs2,ro2,ncodsh,rshmag,rshph)
      at(2)=rshmag*(rt(2)*cos(rshph)-rth(2)*sin(rshph))
      ht(2)=rshmag*(rth(2)*cos(rshph)-rt(2)*sin(rshph))
      if(abs(rshph+pi).lt.eps) go to 9
      if(rshph.gt.eps) ihilb=1
    9 dotar=dot(ai,rt)
c
c vertical incidence can sometimes blow up this step
c    check first
c
      if(abs(dotar).lt.eps) go to 55
      dotar=abs(dotar)/dotar
   55 amb=sqrt(rt(1)*rt(1) + rt(3)*rt(3))*dotar
      doth=dot(ai,rth)
      if(abs(doth).lt.eps) go to 25
      doth=abs(doth)/doth
   25 amh=sqrt(rth(1)*rth(1) + rth(3)*rth(3))*doth
      atmag=rmag*(amb*cos(rph)-amh*sin(rph))
      htmag=rmag*(amh*cos(rph)+amb*sin(rph))
      if(rph.gt.eps) ihilb=1
      at(1)=atmag*r3(1)
      at(3)=atmag*r3(3)
      ht(1)=htmag*r3(1)
      ht(3)=htmag*r3(3)
      call rtoi(at,cosphi,sinphi,qb(3),.true.)
      call rtoi(ht,cosphi,sinphi,qb(3),.true.)
      call coord(at,strike,dip,amaga,'globe',.true.)
      call coord(ht,strike,dip,hmaga,'globe',.true.)
      return
    4 write(ounit,102) va,vb,vp1,vs1,vp2,vs2
  102 format(' ncode = 0 for ',6f6.2)
      return
      end
      */
    public static void ampcal() {
        
    }
    /*
      subroutine rtoi(r,cosp,sinp,qb,dirtcn)
c
c   transforms a vector r from the ray coordinate system
c     to the interface coordinate system and vice versa
c
c   if dirtcn = .true.  ray => interface
c      dirtcn = .false. interface => ray
c
c   qb is the z component of the ray in the interface system
c
      dimension r(1)
      logical dirtcn
      q=abs(qb)/qb
      r(3)=r(3)*(-q)
      if(dirtcn) go to 1
      xr=r(1)*cosp + r(2)*sinp
      yr=r(1)*sinp - r(2)*cosp
      r(1)=xr*q
      r(2)=yr
      return
    1 xr=r(1)*q
      xl=+xr*cosp + r(2)*sinp
      yl= xr*sinp - r(2)*cosp
      r(1)=xl
      r(2)=yl
      return
      end
      */
    public static void rtoi() {
        
    }
    /*
      subroutine rcomp(r3,ncode,nd,sini,incdnt)
c
c   resolves a reflection coeficient r from s/r coef8 into
c     x and z components (in the ray coordinate system)
c     given the resulting ray type:
c       reflected p => ncode = 1
c       reflected s => ncode = 2
c       transmitted p => ncode = 3
c       transmitted s => ncode = 4
c
      dimension r3(1)
      logical incdnt
      cosi=sqrt(1. - sini*sini)
      r3(2)=0.
      if(incdnt) go to 10
      if(nd.ne.0) go to 5
      go to (1,2,3,4) ncode
    1 r3(3)=cosi
      r3(1)=sini
      return
    2 r3(3)=sini
      r3(1)=-cosi
      return
    3 r3(3)=-cosi
      r3(1)=sini
      return
    4 r3(3)=sini
      r3(1)=cosi
      return
    5 go to (6,7,8,9) ncode
    6 r3(3)=cosi
      r3(1)=-sini
      return
    7 r3(3)=-sini
      r3(1)=-cosi
      return
    8 r3(3)=-cosi
      r3(1)=-sini
      return
    9 r3(3)=-sini
      r3(1)=cosi
      return
   10 if(nd.ne.0) go to 11
      go to (3,4) ncode
   11 go to (8,9) ncode
      end
      */
    public static void rcomp() {
        
    }
    /*
      subroutine mkseis(x,y,instrm,qcorr,tq,nft,dt,kst,ihilb)
      complex x(1),wave,fsorce,y(1)
      dimension trap(4)
      logical instrm,qcorr,ounit
      common /innout/ inunit,ounit
      data pi/3.141592654/
      fcut=.004
      nfpts=nft/2 + 1
      fny=1./(2.*dt)
      delf=fny/float(nft/2)
      call dfftr(x,nft,'forward',dt)
      if(ihilb.eq.1) call dfftr(y,nft,'forward',dt)
      if(kst.gt.0) go to 6
    1 isorfn=iask('Pick source wavelet (1-7,not 6): ')
      if(isorfn.eq.6) go to 1
      wave=fsorce(isorfn,0.,0.,kst,a,b,tt,wo,trap)
    6 do 2 i=1,nfpts
         f=float(i-1)*delf
         wave=(1.,0.)
         wave=fsorce(isorfn,f,0.,kst,a,b,tt,wo,trap)
         xr=1.
         xi=0.
         if(.not.instrm) got o 3
            call seisio(f,3000.,xr,xi,+1)
    3    if(.not.qcorr) go to 4
            if(f.lt.fcut) go to 4
               wave=wave*cmplx(exp(-pi*f*tq),0.)
    5          dfac=f*tq*alog(abs(f/fcut)**2-1.)
               dr=cos(dfac)
               di=sin(dfac)
               wave=wave*cmplx(dr,di)
    4     x(i)=wave*x(i)*cmplx(xr,xi)
          if(ihilb.eq.0) go to 2
          x(i)=x(i) + y(i)*cmplx(aimag(wave),-real(wave))*cmplx(xr,xi)
    2 continue
      call dfftr(x,nft,'inverse',delf)
      return
      end
      */
    public static void mkseis() {
        
    }
    /*
      subroutine rayfin(iq,i4,i10,i11,lnumbr,dflag,mflag)
      dimension strike(100),dip(100),z(100),alpha(100),beta(100),
     *          eta(3,100),q(3,5000),q0(3),v(2,100),qv(5000),raydis(3),
     *          qloc1(3),qloc2(3),a(3,3),iface(5000),layer(100),
     *          amag(3,5000),hmag(3,5000),raymag(3,5000),rayhil(3,5000),
     *          raytim(5000),direct(3),rho(100)
      logical pors,amps,free,dflag,mflag
      integer trans,refl,type
      common /cord/ a
      common /amcal/ qloc1,qloc2,vb,va,sinib,sinia,vp1,vs1,rho1,
     *               vp2,vs2,rho2,free,type
      common /transm/ q,qv,v,alpha,beta,rho,strike,dip,iface,jhilb,
     *                amag,hmag,layer,amps,trans,refl,nlyrs
      common /raywrt/ eta,z,raymag,rayhil,raytim,ntim,p0,pors,
     *                oldlyr,q0,direct,tdirec,baz
      call zeerow(raydis,1,3)
      call timdis(raydis,q,3,5000,qv,iq,time,
     *            iface,eta,3,100,z,lnumbr,dislyr,deplyr)
      tmpdis=sqrt(raydis(1)**2 + raydis(2)**2)
      init=1
      if(.not.dflag) go to 1
         tdirec=time
         time=0.
         init=0.
         do 3 i3=1,3
    3    direct(i3)=raydis(i3)
         go to 2
    1 time=time + timcor(direct,raydis,q0,v(1,nlyrs))-tdirec
      if(.not.dflag.and..not.mflag) init=0
    2 call anom(q(1,iq),qv(iq),azanom,panom,angle)
      if(.not.amps) go to 26
         free=.true.
         type=0
         do 52 i52=1,3
   52    qloc1(i52)=q(i52,iq)
         vb=qv(iq)
         sinib=angle
         va=0.
         sinia=0.
         vp1=alpha(1)
         vs1=beta(1)
         rho1=rho(1)
         rho2=0.
         vp2=0.
         vs2=0.
         call ampcal(amag(1,iq),hmag(1,iq),
     *               raymag(1,ntim),rayhil(1,ntim),
     *               0.,0.,ihilb)
         if(ihilb.eq.1) jhilb=1
         free=.false.
         raytim(ntim)=time
         ntim=ntim+1
   26 continue
      write(67,*) ntim,lnumbr,dislyr,deplyr
      call wrtray(lnumbr,azanom,panom,time,baz,p0,pors,
     *            init,i4,i10,i11,oldlyr,angle,tdirec)
      return
      end
      */
    public static void rayfin() {
        
    }
    /*
      subroutine trnsmt(loopst,looped,iq,iv,up)
c
c *******************
c
c     calculates the amplitude of a wave transmitted through
c     a stack of layers
c
c *******************
c
      dimension strike(100),dip(100),alpha(100),beta(100),
     *          rho(100),q(3,5000),v(2,100),qv(5000),
     *          qloc1(3),qloc2(3),a(3,3),iface(5000),layer(100),
     *          amag(3,5000),hmag(3,5000)
      logical amps,free,up
      integer trans,refl,type,itype
      common /cord/ a
      common /amcal/ qloc1,qloc2,vb,va,sinib,sinia,vp1,vs1,rho1,
     *               vp2,vs2,rho2,free,type
      common /transm/ q,qv,v,alpha,beta,rho,strike,dip,iface,jhilb,
     *                amag,hmag,layer,amps,trans,refl,nlyrs
      do 7 i7=loopst,looped-1
         j7=looped - i7 + 1
         if(.not.up) j7=i7
         k7=j7-1
         if(.not.up) k7=k7+1
         call coord(q(1,iq),strike(j7),dip(j7),qloc1,'local',
     *              .false.)
         vb=qv(iq)
         va=v(iv,k7)
         itype=trans
         call snell(qloc1,vb,qloc2,va,itype,sinib,sinia)
c
c   if itype returns as -999, then a problem phase exists
c      iq is flagged for return to main program -- ray will be skipped
c
         if(itype.eq.-999) then
            iq=-999
            return
         endif
         if(.not.amps) go to 19
            vp2=alpha(k7)
            vs2=beta(k7)
            rho2=rho(k7)
            type=trans
            call ampcal(amag(1,iq),hmag(1,iq),
     *                  amag(1,iq+1),hmag(1,iq+1)
     *                 ,strike(j7),dip(j7),ihilb)
            if(ihilb.eq.1) jhilb=1
            vp1=vp2
            vs1=vs2
            rho1=rho2
   19    qv(iq+1)=va
         call coord(qloc2,strike(j7),dip(j7),q(1,iq+1),'globe',
     *              .true.)
         iq=iq+1
         iface(iq)=j7
    7 continue
      return
      end
      */
    public static void trnsmt() {
        
    }
    /*
      subroutine raydwn(iqref,i10,lyref,iq)
c
c **************
c
c     subroutine to reflect a ray from the free surface then
c                propagate it down to a designated interface
c
c **************
c
      dimension strike(100),dip(100),alpha(100),beta(100),
     *          rho(100),q(3,5000),v(2,100),qv(5000),
     *          qloc1(3),qloc2(3),a(3,3),iface(5000),layer(100),
     *          amag(3,5000),hmag(3,5000)
      logical amps,free
      integer trans,refl,type,itype
      common /cord/ a
      common /amcal/ qloc1,qloc2,vb,va,sinib,sinia,vp1,vs1,rho1,
     *               vp2,vs2,rho2,free,type
      common /transm/ q,qv,v,alpha,beta,rho,strike,dip,iface,jhilb,
     *                amag,hmag,layer,amps,trans,refl,nlyrs
      iq=iqref
c
c  take ray down to the reflecting interface --
c
c   do reflection from free surface first
c
      call coord(q(1,iq),strike(1),dip(1),qloc1,'local',
     *           .false.)
      vb=qv(iq)
      va=v(i10,1)
      type=refl
      itype=type
      call snell(qloc1,vb,qloc2,va,itype,sinib,sinia)
c
c   if itype returns as -999, then a problem phase exists
c      iq is flagged for return to main program -- ray will be skipped
c
      if(itype.eq.-999) then
         iq=-999
         return
      endif
      if(.not.amps) go to 20
         free=.true.
         call ampcal(amag(1,iq),hmag(1,iq),
     *               amag(1,iq+1),hmag(1,iq+1),
     *               strike(1),dip(1),ihilb)
         if(ihilb.eq.1) jhilb=1
         free=.false.
   20    qv(iq+1)=va
         call coord(qloc2,strike(1),dip(1),q(1,iq+1),'globe'
     *              ,.true.)
         iq=iq+1
         iface(iq)=1
c
c   now transmit wave down to reflecting interface
c
      if(lyref.eq.2) return
c
c  iq could be returned as -999 from s/r trnsmt -- ray would be skipped
c
      call trnsmt(2,lyref,iq,i10,.false.)
      return
      end
      */
    public static void raydown() {
        
    }
    /*
      subroutine rayup(iqref,i11,lyref,iq)
c
c **************
c
c     subroutine to reflect a ray off an interface at depth then
c                transmit it back up to the free surface
c
c **************
c
      dimension strike(100),dip(100),alpha(100),beta(100),
     *          rho(100),q(3,5000),v(2,100),qv(5000),
     *          qloc1(3),qloc2(3),a(3,3),iface(5000),layer(100),
     *          amag(3,5000),hmag(3,5000)
      logical amps,free
      integer trans,refl,type,itype
      common /cord/ a
      common /amcal/ qloc1,qloc2,vb,va,sinib,sinia,vp1,vs1,rho1,
     *               vp2,vs2,rho2,free,type
      common /transm/ q,qv,v,alpha,beta,rho,strike,dip,iface,jhilb,
     *                amag,hmag,layer,amps,trans,refl,nlyrs
      iq=iqref
      vp1=alpha(lyref-1)
      vs1=beta(lyref-1)
      rho1=rho(lyref-1)
c
c  do the reflection off the interface first
c
      j12=lyref
      call coord(q(1,iq),strike(j12),dip(j12),qloc1,
     *           'local',.false.)
      vb=qv(iq)
      va=v(i11,j12-1)
      type=refl
      itype=type
      call snell(qloc1,vb,qloc2,va,itype,sinib,sinia)
c
c   if itype returns as -999, then a problem phase exists
c      iq is flagged for return to main program -- ray will be skipped
c
      if(itype.eq.-999) then
         iq=-999
         return
      endif
      if(.not.amps) go to 22
         vp2=alpha(j12)
         vs2=beta(j12)
         rho2=rho(j12)
         call ampcal(amag(1,iq),hmag(1,iq),
     *               amag(1,iq+1),hmag(1,iq+1),
     *               strike(j12),dip(j12),ihilb)
         if(ihilb.eq.1) jhilb=1
   22 qv(iq+1)=va
      call coord(qloc2,strike(j12),dip(j12),q(1,iq+1),
     *           'globe',.true.)
      iq=iq+1
      iface(iq)=j12
c
c now transmit wave back to surface
c
      if(lyref.eq.2) return
c
c   iq could be returned as -999 from s/r trnsmt -- ray would be skipped
c
      call trnsmt(2,lyref,iq,i11,.true.)
      return
      end

      */
    public static void rayup() {
        
    }
}
