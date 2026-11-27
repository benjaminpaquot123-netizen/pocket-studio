import sys,json,numpy as np
rows=[json.loads(l) for l in open(sys.argv[1])]
keys=["note","start","tau_ms","click_db","held_ms","dec30_ms","h2_db","h3_db","centroid"]
print(sys.argv[1],"n=",len(rows))
for k in keys:
    v=np.array([r[k] for r in rows if r.get(k) is not None],float)
    if len(v): print(f"  {k:9s} med {np.median(v):7.1f}  IQR {np.percentile(v,25):7.1f}..{np.percentile(v,75):7.1f}  min {v.min():.1f} max {v.max():.1f}")
for c in ["held_capped","dec_capped"]: print(" ",c,np.mean([r[c] for r in rows]))
