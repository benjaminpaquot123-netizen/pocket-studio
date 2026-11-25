import sys,json,numpy as np
rows=[json.loads(l) for l in open(sys.argv[1])]
print(sys.argv[1],"n=",len(rows))
for k in [k for k in rows[0] if k!='file']:
    v=np.array([r[k] for r in rows if r.get(k) is not None],float)
    if len(v): print(f"  {k:9s} med {np.median(v):8.1f}  IQR {np.percentile(v,25):8.1f}..{np.percentile(v,75):8.1f}  (n{len(v)})")
