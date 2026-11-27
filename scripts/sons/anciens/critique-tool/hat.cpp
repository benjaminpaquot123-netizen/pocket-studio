#define private public
#include "DrumKit.h"
#undef private
#include <cstdio>
#include <vector>
#include <string>
#include <cstdlib>
using namespace ps;
static void wav(const std::string& p, const std::vector<float>& x){
  FILE* f=fopen(p.c_str(),"wb"); int sr=48000; int n=(int)x.size(); int bytes=n*4;
  auto w32=[&](int v){fwrite(&v,4,1,f);}; auto w16=[&](short v){fwrite(&v,2,1,f);};
  fwrite("RIFF",1,4,f); w32(36+bytes); fwrite("WAVEfmt ",1,8,f); w32(16); w16(3); w16(1); w32(sr); w32(sr*4); w16(4); w16(32);
  fwrite("data",1,4,f); w32(bytes); fwrite(x.data(),4,n,f); fclose(f);}
int main(int c,char**a){ float dec=atof(a[1]), tone=atof(a[2]), sz=atof(a[3]); int voice=atoi(a[5]);
  DrumKit k; k.prepare(48000); k.setVersion(3);
  if(dec>0){k.closedHat_.SetDecay(dec);} if(tone>0) k.closedHat_.SetTone(tone);
  if(sz>0) k.sizzleClosedCoef_=std::exp(-1.0f/(sz*48000));
  int n=96000; std::vector<float> x(n,0.f);
  for(int i=0;i<n;i+=256){ if(i==0) k.trigger(voice,1.0f,480); k.render(x.data()+i,std::min(256,n-i)); }
  wav(a[4],x);}
