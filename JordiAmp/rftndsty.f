C -------------------------------------------------------------------- C 
C This program computes density contrasts from receiver function       C
C amplitudes.                                                          C
C -------------------------------------------------------------------- C 
C Last update:  08/10/2005, JJC                                        C
C                                                                      C
C Comments:                                                            C
C  1) The 'Initialization of large (1000016-unit) aggregate area       C
C     `lsqd' at (^) slow and takes lots of memory during g77 compile'  C
C     WARNING obtained during compilation has to do with merging the   C
C     s(NVSMAX,NRHMAX) array with DATA-initialized variables in the    C
C     same COMMON statement. Not initializing anything in the COMMON   C
C     statement removes the WARNING.                                   C
C -------------------------------------------------------------------- C
C 
      PARAMETER (NWMAX=250, NTMAX=4096)
C
      INTEGER   i, j, iwav, nwav, istat, pgopen, npts, nerr, nrayp,
     &          nboot
      REAL*4    x2min, x2max, y2min, y2max, x1min, x1max, y1min, y1max,
     &          rftn(NTMAX), beg, del, rayp, ampl(NWMAX,5), getvs0, xc,
     &          vp0, vs0, vp1, vs1, rh1, vp2, vs2, rh2, vp0n, vs0n, yc,
     &          vp1n, vs1n, rh1n, vp2n, vs2n, rh2n, vp1o, vs1o, rh1o,
     &          vp2o, vs2o, rh2o, tmin, tmax, sigvs0, sigdvs, sigdrh,
     &          baz
      LOGICAL   lexist
      CHARACTER infile(NWMAX)*25, key*1, kevtname*25
C
      COMMON /model/ vp0, vs0, vp1, vs1, rh1, vp2, vs2, rh2
      COMMON /wvfrm/ rftn, beg, del, npts, rayp, baz, kevtname
C
      COMMON /panel1/ x1min, x1max, y1min, y1max
      COMMON /panel2/ x2min, x2max, y2min, y2max
C
C Initializations
C
c      DATA vp1/6.5/, vs1/3.7530/, rh1/2.85/, vp2/8.1/, vs2/4.6760/, 
c     &     rh2/3.362/, vp0/6.5/, vs0/3.7530/
C Aki and Richards values
      DATA vp1/6.0/, vs1/3.50/, rh1/3.00/, vp2/7.0/, vs2/4.20/, 
     &     rh2/4.00/, vp0/6.0/, vs0/3.50/
C hpc
      REAL*4 a1, a2, a3, a4
      rayp = 0.03
      print *, vp1, vs1, rh1
      print *, vp2, vs2, rh2
      print *, rayp
      CALL amplitude(rayp, a1, a2, a3, a4)
      END

      SUBROUTINE amplitude(rayp,a1,a2,a3,a4)
C -------------------------------------------------------------------- C 
C Calculates the receiver function amplitudes for a given ray para-    C
C meter.                                                               C
C -------------------------------------------------------------------- C 
C Last update: 08/08/2005, Jordi Julia                                 C
C -------------------------------------------------------------------- C 
C
      REAL*4    rayp, a1, a2, a3, a4, vp1, vp2, vs1, vs2, rh1, rh2,
     &          etap1, etap2, etas1, etas2, a, b, c, d, E, F, G, H, DD,
     &          PP, PS, c1, c2, PP0, PP2, PS2, PS0, SS, rsr0, zsz0, 
     &          rpz0, vp0, vs0
C
      COMMON /model/ vp0,vs0,vp1,vs1,rh1,vp2,vs2,rh2
C
C Slownesses
C
      etap0=SQRT(1./vp0/vp0-rayp*rayp)
      etas0=SQRT(1./vs0/vs0-rayp*rayp)
      etap1=SQRT(1./vp1/vp1-rayp*rayp)
      etap2=SQRT(1./vp2/vp2-rayp*rayp)
      etas1=SQRT(1./vs1/vs1-rayp*rayp)
      etas2=SQRT(1./vs2/vs2-rayp*rayp)
C
C Free surface coefficients
C
      c1=(1./vs0/vs0-2.*rayp*rayp)
      c2=4.*rayp*rayp*etap0*etas0
      PP0=(-c1*c1+c2)/(c1*c1+c2)
      PS0=4.*vp0/vs0*rayp*etap0*c1/(c1*c1+c2)
      SP0=4.*vs0/vp0*rayp*etas0*c1/(c1*c1+c2)
C
C Reflection/Transmission coefficients
C
      a=rh2*(1.-2.*vs2*vs2*rayp*rayp)-rh1*(1.-2.*vs1*vs1*rayp*rayp)
      b=rh2*(1.-2.*vs2*vs2*rayp*rayp)+2.*rh1*vs1*vs1*rayp*rayp
      c=rh1*(1.-2.*vs1*vs1*rayp*rayp)+2.*rh2*vs2*vs2*rayp*rayp
      d=2.*(rh2*vs2*vs2-rh1*vs1*vs1)
C
      E=b*etap1+c*etap2
      F=b*etas1+c*etas2
      G=a-d*etap1*etas2
      H=a-d*etap2*etas1
      DD=E*F+G*H*rayp*rayp
C
      PP=2.*rh2*etap2*F*vp2/vp1/DD
      PS=-2.*rh2*etap2*G*rayp*vp2/vs1/DD
      PS2=-2.*etap1*(a*b+c*d*etap2*etas2)*rayp*vp1/vs1/DD
      PP2=((b*etap1-c*etap2)*F-(a+d*etap1*etas2)*H*rayp*rayp)/DD
      SP2=-2.*etas1*(a*b+c*d*etap2*etas2)*rayp*vs1/vp1/DD
      SS=-((b*etas1-c*etas2)*E-(a+d*etap2*etas1)*G*rayp*rayp)/DD
C
C Amplitudes
C
      rsr0=vs0*c1/2./vp0/rayp/etap0
      zsz0=-2.*rayp*etas0*vs0/c1/vp0
      rpz0=2.*rayp*etas0/(1./vs0/vs0-2.*rayp*rayp)
C
      a1=rpz0
      a2=rpz0*(rsr0-zsz0)*PS/PP
      a3=rpz0*(rsr0-zsz0)*PP0*PS2-a2*PP0*PP2
      a4=rpz0*(rsr0-zsz0)*PS0*SS-a2*(PP0*PS2+PS0*SP2+PS*SP0*PP2/PP)
     &                                   +rpz0*(rsr0-zsz0)*PP0*PS2*PS/PP
C
      print *,a1, a2, a3, a4
      print *, rsr0, zsz0, rpz0
      print *, PP0, PS0, SP0
      print *, PP, PS
      print *, PP2, PS2, SP2, SS
      print *, c1, c2
      END
