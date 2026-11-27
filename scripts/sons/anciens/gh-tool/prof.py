import sys,glob,os
sys.path.insert(0,sys.argv[1]); import numpy as np, kick_measure as km
from scipy import signal
P=[]
for f in sorted(glob.glob(sys.argv[2])):
    x=km.load(f); on=int(np.argmax(np.abs(x)>np.abs(x).max()*0.05))
    lo=signal.sosfiltfilt(km.LP150,x[on:on+int(0.5*km.SR)]); e=km.henv(lo,3); d=20*np.log10(e/e.max())
    P.append([d[int(ms*48)] for ms in (25,50,75,100,150,200,250,300)])
P=np.array(P); print(sys.argv[3], "n",len(P)," ".join(f"{v:6.1f}" for v in np.median(P,0)), "| IQR75%:", " ".join(f"{v:6.1f}" for v in np.percentile(P,75,0)))
