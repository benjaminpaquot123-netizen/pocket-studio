import sys,subprocess,glob,os,json
import numpy as np
from scipy import signal
SR=48000
def load(p):
    r=subprocess.run(["ffmpeg","-v","quiet","-i",p,"-ac","1","-ar",str(SR),"-f","f32le","-"],capture_output=True).stdout
    return np.frombuffer(r,np.float32).astype(float)
def rmsenv(x,ms=5):
    n=int(SR*ms/1000); return np.sqrt(np.convolve(x*x,np.ones(n)/n,'same')+1e-20)
def times(x):
    e=rmsenv(x); pk=int(np.argmax(e)); d=20*np.log10(e/e[pk]); out={}
    for v in (-20,-40):
        w=np.where(d[pk:]<v)[0]; out[v]=(w[0]+pk)/SR*1000 if len(w) else np.nan  # from file start-ish
    on=int(np.argmax(e>e[pk]*0.1)); out['on']=on/SR*1000
    for v in (-20,-40): out[v]-=out['on']
    return out,on
def spec(x,on):
    s=x[on:on+int(0.3*SR)]; X=np.abs(np.fft.rfft(s*np.hanning(len(s))))**2; f=np.fft.rfftfreq(len(s),1/SR)
    m=(f>20)&(f<20000); tot=X[m].sum()
    return dict(cent=(f[m]*X[m]).sum()/tot, centA=(f[m]*np.sqrt(X[m])).sum()/np.sqrt(X[m]).sum(), hi=10*np.log10(X[f>10000].sum()/tot), b25=10*np.log10(X[(f>2000)&(f<5000)].sum()/tot))
def h2(x):
    e=rmsenv(x); on=int(np.argmax(e>e.max()*0.1))
    s=x[on+int(0.06*SR):on+int(0.16*SR)]; N=1<<16
    X=np.abs(np.fft.rfft(s*np.hanning(len(s)),N)); f=np.fft.rfftfreq(N,1/SR)
    m=(f>35)&(f<90); i=np.argmax(np.where(m,X,0)); f0=f[i]
    def pk(fc): mm=(f>fc*0.92)&(f<fc*1.08); return X[mm].max()
    return f0,20*np.log10(pk(2*f0)/X[i]),20*np.log10(pk(3*f0)/X[i])
mode=sys.argv[1]; R=[]
for p in sys.argv[2:]:
    x=load(p)
    if mode=='kick': R.append(h2(x)+(len(x)/SR*1000,))
    else:
        t,on=times(x); s=spec(x,on); R.append((t[-20],t[-40],s['cent'],s['centA'],s['hi'],s['b25'],len(x)/SR*1000))
R=np.array(R,float); print(mode,len(R)); print("median",np.round(np.nanmedian(R,0),1)); print("q25",np.round(np.nanpercentile(R,25,0),1)); print("q75",np.round(np.nanpercentile(R,75,0),1))
