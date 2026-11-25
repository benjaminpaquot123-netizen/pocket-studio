import sys,glob,numpy as np
from scipy.io import wavfile
from scipy.signal import hilbert,butter,sosfilt
for f in sorted(glob.glob(sys.argv[1]+'/*.wav')):
    sr,x=wavfile.read(f); x=x.astype(float); x=x.mean(1) if x.ndim>1 else x; x/=np.abs(x).max()+1e-12
    env=np.abs(hilbert(x)); w=int(sr*0.0005); env=np.convolve(env,np.ones(w)/w,'same')
    edb=20*np.log10(env+1e-9); pk=np.argmax(env)
    # bursts: local rises in first 60 ms
    e=env[:int(.06*sr)]; d=np.diff(e); peaks=[i for i in range(1,len(e)-1) if e[i]>e[i-1] and e[i]>=e[i+1] and e[i]>0.3]
    bur=[];  [bur.append(p) for p in peaks if not bur or p-bur[-1]>int(.003*sr)]
    t60=np.argmax(edb[pk:]< -60)/sr if (edb[pk:]< -60).any() else len(x)/sr
    t20=np.argmax(edb[pk:]< -20)/sr
    X=np.abs(np.fft.rfft(x*np.hanning(len(x)))); fr=np.fft.rfftfreq(len(x),1/sr)
    cen=(fr*X).sum()/X.sum(); pkf=fr[np.argmax(X)]
    band=lambda a,b:10*np.log10((X[(fr>=a)&(fr<b)]**2).sum()+1e-12)
    tot=band(20,20000)
    bands={k:round(band(*v)-tot,1) for k,v in {'<200':(20,200),'200-800':(200,800),'0.8-2k':(800,2000),'2-5k':(2000,5000),'5-10k':(5000,10000),'>10k':(10000,20000)}.items()}
    rms=20*np.log10(np.sqrt((x**2).mean()))
    print(f.split('-')[-1][:-4],f"sr={sr} dur={len(x)/sr*1000:.0f}ms bursts={len(bur)} gaps_ms={[round((b-bur[0])/sr*1000,1) for b in bur]} t-20={t20*1000:.0f}ms t-60={t60*1000:.0f}ms centroid={cen:.0f}Hz peakf={pkf:.0f}Hz crest={20*np.log10(1/np.sqrt((x**2).mean())):.1f}dB bands={bands}")
