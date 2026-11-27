import sys,json,glob,os
sys.path.insert(0,sys.argv[1]); import numpy as np, kick_measure as km
from scipy import signal
SR=48000
def bands(x):
    X=np.abs(np.fft.rfft(x*np.hanning(len(x))))**2; fr=np.fft.rfftfreq(len(x),1/SR)
    tot=X[(fr>=20)&(fr<20000)].sum()
    B={'<150':(20,150),'150-400':(150,400),'400-1k':(400,1000),'1-2k':(1000,2000),'2-5k':(2000,5000),'5-10k':(5000,10000),'>10k':(10000,20000)}
    r={k:round(10*np.log10(X[(fr>=a)&(fr<b)].sum()/tot+1e-12),1) for k,(a,b) in B.items()}
    cen=float((fr*X).sum()/X.sum()); return r,cen
for f in sorted(glob.glob(sys.argv[2])):
    x=km.load(f); 
    if not len(x): continue
    x=x/(np.abs(x).max()+1e-12)
    e=km.henv(x,1.0); pk=int(np.argmax(e)); ed=20*np.log10(e/e[pk])
    def t(dbv):
        w=np.where(ed[pk:]<dbv)[0]; return round(w[0]/SR*1000,1) if len(w) else None
    on=int(np.argmax(e>e[pk]*0.05))
    seg=x[on:on+int(0.5*SR)]
    r,cen=bands(seg)
    # first 30 ms bands vs whole
    # bursts: peaks of 0.5 ms env in first 60 ms
    e2=km.henv(x,0.5)[on:on+int(0.06*SR)]; p,_=signal.find_peaks(e2,height=0.3*e2.max(),distance=int(0.003*SR),prominence=0.1*e2.max())
    # body: strongest peak 100-400 Hz in first 80 ms
    s=x[on:on+int(0.08*SR)]; S=np.abs(np.fft.rfft(s*np.hanning(len(s)),8192*4)); fr=np.fft.rfftfreq(8192*4,1/SR)
    m=(fr>100)&(fr<400); body=float(fr[m][np.argmax(S[m])])
    print(json.dumps(dict(file=os.path.basename(f),dur_ms=round(len(x)/SR*1000),t6=t(-6),t20=t(-20),t40=t(-40),t60=t(-60),attack_ms=round((pk-on)/SR*1000,1),bursts=len(p),centroid=round(cen),body_hz=round(body),**r)))
