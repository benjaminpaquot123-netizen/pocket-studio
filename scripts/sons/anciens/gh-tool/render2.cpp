#include "DrumKit.h"
#include <cstdio>
#include <vector>
#include <string>
using namespace ps;
static void wav(const std::string& p, const std::vector<float>& x){
  FILE* f=fopen(p.c_str(),"wb"); int sr=48000; int n=(int)x.size(); int bytes=n*4;
  auto w32=[&](int v){fwrite(&v,4,1,f);}; auto w16=[&](short v){fwrite(&v,2,1,f);};
  fwrite("RIFF",1,4,f); w32(36+bytes); fwrite("WAVEfmt ",1,8,f); w32(16); w16(3); w16(1); w32(sr); w32(sr*4); w16(4); w16(32);
  fwrite("data",1,4,f); w32(bytes); fwrite(x.data(),4,n,f); fclose(f);}
struct P{int param; float v;};
static void one(const char* name,int voice,int kind,std::vector<P> ps,float secs=2.0f){
  DrumKit k; k.prepare(48000); k.setVersion(3); if(kind>=0) k.setKind(voice,kind);
  for(auto&p:ps) k.setVoiceParam(voice,p.param,p.v);
  int n=(int)(secs*48000); std::vector<float> x(n,0.f); const int B=256;
  for(int i=0;i<n;i+=B){ if(i==480) {} if(i==0) k.trigger(voice,1.0f,480); k.render(x.data()+i,std::min(B,n-i)); }
  wav(std::string(OUT)+"/"+name+".wav",x);}
int main(){ using D=DrumKit; one("x_sub0",D::Kick,D::KickStudio,{{D::Tune,52.f},{D::Sub,0.f}}); one("x_sub1",D::Kick,D::KickStudio,{{D::Tune,52.f},{D::Sub,1.f}}); return 0;
  using D=DrumKit;
  one("kick_v3default",D::Kick,-1,{});
  one("kick_house_F",D::Kick,D::KickStudio,{{D::Tune,43.65f}});
  one("kick_house_A",D::Kick,D::KickStudio,{{D::Tune,55.0f}});
  one("kick_soft",D::Kick,D::KickStudio,{{D::Tune,52.f},{D::Click,0.25f}});
  one("kick_trap",D::Kick,D::KickStudio,{{D::Tune,52.f},{D::Decay,0.2f}});
  one("kick_dnb",D::Kick,D::KickStudio,{{D::Tune,52.f},{D::Decay,0.18f},{D::Click,0.7f}});
  one("kick_hard",D::Kick2,-1,{});
  one("kick_hard_07",D::Kick,D::KickHard,{{D::Tune,52.f},{D::Drive,0.7f}});
  // 808 trap in F (29 -> 43.65 Hz), version 1
  one("b808_trap",D::Kick2,D::Bass808,{{D::Tune,29},{D::Decay,1.3f},{D::Drive,0.7f},{D::Click,0.5f},{D::Tone,0.5f},{D::Glide,0.15f},{D::Version,1},{D::PitchDrop,24},{D::DropTime,0.025f}},3.0f);
  one("b808_default",D::Kick2,D::Bass808,{},3.0f);
  const char* names[]={"","","snare","snare2","clap","chat","ohat","lowtom","midtom","hightom","rim","cowbell","cymbal","shaker","conga","zap"};
  for(int v=2;v<16;++v) one(names[v],v,-1,{},2.0f);
}
