# known kick: note 50 Hz, start 150 Hz, tau 20 ms, h2 -20 dB, h3 -30 dB, flat hold then cut
import numpy as np, sys, struct
sr=48000; t=np.arange(int(0.6*sr))/sr
f=50+100*np.exp(-t/0.020); ph=2*np.pi*np.cumsum(f)/sr
a=np.where(t<0.12,1.0,np.exp(-(t-0.12)/0.025))
x=a*(np.sin(ph)+0.1*np.sin(2*ph)+0.0316*np.sin(3*ph))
x=np.concatenate([np.zeros(480),x])*0.5
d=x.astype(np.float32).tobytes()
open(sys.argv[1],'wb').write(b'RIFF'+struct.pack('<I',36+len(d))+b'WAVEfmt '+struct.pack('<IHHIIHH',16,3,1,sr,sr*4,4,32)+b'data'+struct.pack('<I',len(d))+d)
