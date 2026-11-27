import sys,glob,numpy as np,warnings; warnings.filterwarnings('ignore')
from scipy.io import wavfile
from scipy.signal import hilbert
R=[]
for f in sorted(glob.glob(sys.argv[1]+'/**/Claps/*.wav',recursive=True)):
    sr,x=wavfile.read(f); x=x.astype(float); x=x.mean(1) if x.ndim>1 else x; x/=np.abs(x).max()+1e-12
    env=np.abs(hilbert(x)); w=int(sr*.002); env=np.convolve(env,np.ones(w)/w,'same'); e=20*np.log10(env/env.max()+1e-9); pk=np.argmax(env)
    t20=np.argmax(e[pk:]<-20)/sr*1000; t40=(np.argmax(e[pk:]<-40)/sr*1000) if (e[pk:]<-40).any() else len(x)/sr*1000
    X=np.abs(np.fft.rfft(x)); fr=np.fft.rfftfreq(len(x),1/sr); P=X**2
    cen=(fr*P).sum()/P.sum(); low=10*np.log10(P[fr<250].sum()/P.sum()+1e-12)
    R.append((t20,t40,cen,low))
R=np.array(R); print('n',len(R))
for i,k in enumerate(['t-20ms','t-40ms','centroid Hz','<250Hz dB']): q=np.percentile(R[:,i],[10,25,50,75,90]); print(k,np.round(q,0))
def cls(r):
    t20,t40,c,l=r
    if t40>500: return 'reverb tail'
    if c>5000 and t20<80: return 'tight bright'
    if l>-15: return 'with body/thump'
    if c<3000: return 'dark/warm'
    return 'classic mid'
from collections import Counter; c=Counter(cls(r) for r in R); print(c)
