import sys, json, glob, os
sys.path.insert(0, sys.argv[1])
import numpy as np, kick_measure as km
out = []
for f in sorted(glob.glob(sys.argv[2])):
    x = km.load(f)
    o = max(0, int(np.argmax(np.abs(x) > np.abs(x).max()*0.05)) - int(0.001*km.SR))
    m = km.measure(x, o, None, "iso")
    if m: m["file"] = os.path.basename(f); out.append(m)
for m in out: print(json.dumps({k:(round(v,1) if isinstance(v,float) else v) for k,v in m.items()}))
