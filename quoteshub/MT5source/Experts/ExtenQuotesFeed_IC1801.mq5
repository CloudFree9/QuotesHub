//+------------------------------------------------------------------+
//|                                              ExtenQuotesFeed.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+


#property copyright "Copyright 2017, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"

#include "socketlib.mqh"

//--- input parameters
input string   underlying="CTP_IC1801";
input string   Host        = "0.0.0.0";
input string   Title       = "CTP期货IC1801";
input ushort   Port        = 1998;
double         lastDeal    = 2689;
double         lastBid     = 2688;
double         lastAsk     = 2689;
ulong          lastVolume  = 1000;   

SOCKET64 server=INVALID_SOCKET64;

void getTick(string msg, MqlTick &tick) {
   tick.time = TimeLocal();
   tick.time_msc = tick.time * 1000;
   double delta = MathRand() * 2.0 / 32767  - 1;
   lastDeal += delta;
   lastBid += delta;
   lastAsk += delta;
   
   tick.ask = lastAsk;
   tick.bid = lastBid;
   tick.last = lastDeal;
   
   tick.volume = lastVolume;
   tick.flags = TICK_FLAG_BID | TICK_FLAG_ASK | TICK_FLAG_LAST | TICK_FLAG_BUY;

}

void CloseClean() {
   printf("Shutdown server");

   if(server!=INVALID_SOCKET64) { 
      closesocket(server); 
      server=INVALID_SOCKET64; 
   } // close the server

   WSACleanup();
}

void drawTitle() {

   ObjectCreate(0,"TITLE",OBJ_BITMAP_LABEL,0,0,0);
//--- 指定用于在CLOCK物件编写的图形资源的名称
   ObjectSetString(0,"TITLE",OBJPROP_BMPFILE,"::IMG");
   
   int w = 300, h = 30;
   uint ExtImg[300*30];
   ArrayFill(ExtImg , 0, 300*30, 0);
   TextSetFont("宋体",-96,FW_EXTRABOLD,0);
   
   TextOut(Title, 25, 5, TA_LEFT|TA_TOP, ExtImg, w, h, 0xFFFFFF00, COLOR_FORMAT_XRGB_NOALPHA);
   ResourceCreate("::IMG",ExtImg,w,h,0,0,w,COLOR_FORMAT_XRGB_NOALPHA);
      //--- 强制图表更新
   ChartRedraw();
   
 }

void registerInstrument() {
}

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit() {

   char wsaData[]; 
   ArrayResize(wsaData, sizeof(WSAData));
   int res = WSAStartup(MAKEWORD(2,2), wsaData);
   if(res != 0) { 
      Print("错误 - WSAStartup 初始化失败: "+string(res)); 
      return INIT_FAILED; 
   }
   
   // create a socket
   server=socket(AF_INET,SOCK_DGRAM, IPPROTO_UDP);
         
   if(server == INVALID_SOCKET64) { 
      Print("错误 - Socket创建失败: "+WSAErrorDescript(WSAGetLastError()));
      CloseClean();
      return INIT_FAILED; 
   }
   
   // bind to address and port
   Print("正在绑定主机和端口..."+Host+":" + string(Port));
   
   char ch[]; 
   StringToCharArray(Host,ch);
   sockaddr_in addrin;
   addrin.sin_family=AF_INET;
   addrin.sin_addr.u.S_addr=inet_addr(ch);
   addrin.sin_port=htons(Port);
   ref_sockaddr ref; 
   ref.in=addrin;
       
   if(bind(server,ref.ref,sizeof(addrin)) == SOCKET_ERROR) {
      int err=WSAGetLastError();
      if(err!=WSAEISCONN) { 
         Print("绑定失败: " + WSAErrorDescript(err) + ", 进行清理工作..."); 
         CloseClean(); 
         return INIT_FAILED; 
      }
   }
   

   int block_mode = 1;
   res=ioctlsocket(server,(int)FIONBIO,block_mode);
         
   if(res != NO_ERROR) { 
      Print("ioctlsocket 失败: " + string(res) + ", 进行清理工作..."); 
      CloseClean(); 
      return INIT_FAILED; 
   }
   
   Print("服务器启动成功！");

   EventSetTimer(1);
//---
   drawTitle();   

   return(INIT_SUCCEEDED);
  }
//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
  {
//--- destroy timer
   EventKillTimer();
   Print("关闭报价服务, 进行清理工作..."); 
   CloseClean(); 
   Print("清理完成, Bye!"); 
   
  }
//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick()
  {
//---
   
  }
//+------------------------------------------------------------------+
//| Timer function                                                   |
//+------------------------------------------------------------------+
void OnTimer() {
//---
      static uchar buf[1024] = {0};
      static uint nextpos = 0;
      

      if(server!=INVALID_SOCKET64) {
         
         uchar content[];
         ref_sockaddr ref={0}; 
         int len=ArraySize(ref.ref);
         
         for (int j=0;j<10;j++) {
            int res=recvfrom(server,buf,1024,0,ref.ref,len);
            if (res>0) {
               uint l = buf[0];
               l = (l << 8) + buf[1];
               l = (l << 8) + buf[2];
               l = (l << 8) + buf[3];
               
               if (res > 1024 || l > 1024 ||  res != (l + 4)) {
                  Print("无效输入,请检查报价输入端是否有异常.");
                  OnDeinit(0);
                  return;
               }
                  
               ArrayResize(content, l+1);
               content[l] = 0;
               ArrayCopy(content, buf, 0, 4, l);
               
               string msg = CharArrayToString(content);
               string result[];
               StringSplit(msg, ',', result);
               
               
   
               MqlTick tick[1];

               tick[0].ask       = StringToDouble(result[0]);
               tick[0].bid       = StringToDouble(result[1]);
               tick[0].last      = StringToDouble(result[2]);
               tick[0].volume    = StringToInteger(result[3]);
               tick[0].time_msc  = StringToInteger(result[5]) - TimeGMTOffset() * 1000;
               tick[0].time      = tick[0].time_msc / 1000;

               tick[0].flags     = TICK_FLAG_BID | TICK_FLAG_ASK | TICK_FLAG_LAST | TICK_FLAG_SELL;

               Print("GMT time:", TimeGMT(), "Quote | bid:", tick[0].bid, ", ask:", tick[0].ask, ", price:", tick[0].last, ", time:", tick[0].time);
               int ret = CustomTicksAdd(_Symbol, tick);
               if (ret != 1) {
                  Print("The return code : '", ret, "' error code:'", GetLastError(), "'");
               }
            } else {
               return;
            }
         }
      } else {
        Print("Socket 无效,可能已被意外中断,请重启EA");
        return;
      }
   
  }
//+------------------------------------------------------------------+
