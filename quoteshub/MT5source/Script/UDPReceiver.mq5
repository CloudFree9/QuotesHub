//+------------------------------------------------------------------+
//|                                                  UDPReceiver.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2017, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"
#include "..\Include\socketlib.mqh"
#include "..\Include\JAson.mqh"

SOCKET64 server=INVALID_SOCKET64;
int Port = 33333;

//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+

void CloseClean() {
   printf("Shutdown server");

   if(server!=INVALID_SOCKET64) { 
      closesocket(server); 
      server=INVALID_SOCKET64; 
   } // close the server

   WSACleanup();
}

void OnStart()
  {
  
   char wsaData[]; 
   ArrayResize(wsaData, sizeof(WSAData));
   int res = WSAStartup(MAKEWORD(2,2), wsaData);
   if(res != 0) { 
      Print("错误 - WSAStartup 初始化失败: " + string(res)); 
      return; 
   }
  
//---
   // create a socket
   server=socket(AF_INET,SOCK_DGRAM, IPPROTO_UDP);
         
   if(server == INVALID_SOCKET64) { 
      Print("错误 - Socket创建失败: "+WSAErrorDescript(WSAGetLastError()));
      CloseClean();
      return; 
   }
   
   string Host = "127.0.0.1";  
   
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
         Print("端口绑定失败: " + WSAErrorDescript(err) + ", 进行清理工作..."); 
         CloseClean(); 
         return; 
      }
   }
   
   int len = sizeof(addrin);
   if (getsockname(server, ref.ref, len) != 0){
      Print("端口绑定失败, 进行清理工作..."); 
      CloseClean(); 
      return; 
   }

   Port = ntohs(ref.in.sin_port);

   // bind to address and port
   Print("正在绑定主机和端口..."+Host+string(Port));

   Print("Port = " + string(Port));
   
   int block_mode = 1;
   res=ioctlsocket(server,(int)FIONBIO,block_mode);
         
   if(res != NO_ERROR) { 
      Print("ioctlsocket 失败: " + string(res) + ", 进行清理工作..."); 
      CloseClean(); 
      return; 
   }
   

   Print("本地服务启动成功！");
   
   while( true ) {
      OnTimer();
   }

  }
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
                  return;
               }
                  
               ArrayResize(content, l+1);
               content[l] = 0;
               ArrayCopy(content, buf, 0, 4, l);
               
               string msg = CharArrayToString(content);
               string result[];
               StringSplit(msg, ',', result);
               
               
   
               MqlTick tick[1];

               string symbol            = result[0];
               int conid             = StringToInteger(result[1]);
               tick[0].ask       = StringToDouble(result[2]);
               tick[0].bid       = StringToDouble(result[3]);
               tick[0].last      = StringToDouble(result[4]);
               tick[0].volume    = StringToInteger(result[5]);
               tick[0].time  = StringToInteger(result[7]) - TimeGMTOffset();
//               tick[0].time_msc  = StringToInteger(result[7]) - TimeGMTOffset() * 1000;
               tick[0].time_msc      = tick[0].time * 1000;

               tick[0].flags     = TICK_FLAG_BID | TICK_FLAG_ASK | TICK_FLAG_LAST | TICK_FLAG_SELL;

               Print("Local time:", TimeLocal(), ", Symbol:", symbol, ", Conid:", conid);
               Print("Quote | price:", tick[0].last, ", volume:", tick[0].volume, ", Bar time:", tick[0].time);

            } else {

               Sleep(5000);
               return;
            }
         }
      } else {
        Print("Socket 无效,可能已被意外中断,请重启EA");
        return;
      }
   
  }
//+------------------------------------------------------------------+
