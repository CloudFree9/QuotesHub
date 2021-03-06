//+------------------------------------------------------------------+
//|                                            registerIBsymbols.mq5 |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2018, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"
#include "..\Include\socketlib.mqh"
#include "..\Include\JAson.mqh"

SOCKET64 server=INVALID_SOCKET64;
string Host = "127.0.0.1";
string RegURL = "http://127.0.0.1/ibquotes/realtimebar";
string IBSymbols[];
string ContractIds[];

int Port = 0;

void CloseClean() {
   printf("Shutdown server");

   if(server!=INVALID_SOCKET64) { 
      closesocket(server); 
      server=INVALID_SOCKET64; 
   } // close the server

   WSACleanup();
}

int NetInit() {

   char wsaData[]; 
   ArrayResize(wsaData, sizeof(WSAData));
   int res = WSAStartup(MAKEWORD(2,2), wsaData);
   if(res != 0) { 
      Print("错误 - WSAStartup 初始化失败: " + string(res)); 
      return -1; 
   }
  
//---
   // create a socket
   server=socket(AF_INET,SOCK_DGRAM, IPPROTO_UDP);
         
   if(server == INVALID_SOCKET64) { 
      Print("错误 - Socket创建失败: "+WSAErrorDescript(WSAGetLastError()));
      CloseClean();
      return -1; 
   }
   
   
   char ch[]; 
   StringToCharArray(Host,ch);
   sockaddr_in addrin;
   addrin.sin_family=AF_INET;
   addrin.sin_addr.u.S_addr=inet_addr(ch);
   ref_sockaddr ref; 
   ref.in=addrin;
       
   if(bind(server,ref.ref,sizeof(addrin)) == SOCKET_ERROR) {
      int err=WSAGetLastError();
      if(err!=WSAEISCONN) { 
         Print("端口绑定失败: " + WSAErrorDescript(err) + ", 进行清理工作..."); 
         CloseClean(); 
         return -1; 
      }
   }
   
   int len = sizeof(addrin);
   if (getsockname(server, ref.ref, len) != 0){
      Print("端口绑定失败, 进行清理工作..."); 
      CloseClean(); 
      return -1; 
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
      return -1; 
   }
   

   Print("本地服务启动成功！");
   return 0;

}

void StartListening() {
   while( true ) {
      ReceiveQuoteMsgs();
   }

}


//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
//---
      int net = NetInit();
      
      if (net <0) {
         MessageBox("网络服务初始化失败！", "错误");
         return;
      }

      int id_num = GetAllIBSymbolIDs(IBSymbols, ContractIds);
      
      if (id_num <=0) {
         MessageBox("未找到相应标的定义！请检查自定义标的。", "错误");
         return;
      }
      
      
      if(RegisterInstrument(GetAllIBSymbolIdList()) == -1) {
         MessageBox("注册自定义标的出错！", "严重错误");
         return;
      }
      
      StartListening();
      
  }
//+------------------------------------------------------------------+

void GetCustomSymbols(string provider, string &res[]) {

   int c = SymbolsTotal(false);
  
   for (int j=0;j<c;j++) {
   
      string n = SymbolName(j, false);
      string p = SymbolInfoString(n, SYMBOL_PATH);
      string res1[];
      int i = StringSplit(p, StringGetCharacter("\\", 0), res1);
      
      if (i<2 || (res1[0] != "Custom" && res1[0] != "IB")) {
         continue;
      }
      
      ArrayResize(res, ArraySize(res) + 1);
      res[ArraySize(res) - 1] = n;
      
   }
}

string GetContractIdBySymbol(string sym) {

   for(int i=0;i<ArraySize(ContractIds);i++) {
      if (IBSymbols[i] == sym) return ContractIds[i];
   }
   
   return "";
}

string GetSymbolByContractId(string id) {

   for(int i=0;i<ArraySize(ContractIds);i++) {
      if (ContractIds[i] == id) return IBSymbols[i];
   }
   
   return "";
}

string GetSymbolByBasis(string id) {

   for(int i=0;i<ArraySize(IBSymbols);i++) {
      string basis = SymbolInfoString(IBSymbols[i], SYMBOL_BASIS);
      if (basis == id) return IBSymbols[i];
   }
   
   return "";
}


bool SymbolExists(string provider, string s) {
   string res[];
   GetCustomSymbols(provider, res);
   
   for(int i=0;i<ArraySize(res);i++) {
      if (res[i] == s) return true;
   }
   
   return false;
}

int GetAllIBSymbolIDs(string &syms[], string &contractids[]) {
   
   string res = "";
//   string syms[];
   GetCustomSymbols("IB", syms);
   
   ArrayResize(contractids, ArraySize(syms));
   for (int i=0;i<ArraySize(syms);i++) {   
      contractids[i] = SymbolInfoString(syms[i], SYMBOL_BASIS);
   }
   
   return ArraySize(syms);
}

string GetAllIBSymbolIdList() {
   string list = "";
   for (int i=0;i<ArraySize(ContractIds);i++) {   
      if (i!=0) list = list + ",";
      list = list + ContractIds[i];
   }
   return list;
}
void ReceiveQuoteMsgs() {
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
               MqlRates rate[1];

               int conid             = StringToInteger(result[2]);
               int type          = StringToInteger(result[0]);
               string symbol;
               
               if (conid == -1) {
                  symbol = GetSymbolByBasis(result[1]);
               } else {
                  symbol = GetSymbolByContractId(conid);
               }
               
               if (symbol == "") continue;
               
               if (type == 0) {
                  tick[0].ask       = StringToDouble(result[2]);
                  tick[0].bid       = StringToDouble(result[3]);
                  tick[0].last      = StringToDouble(result[4]);
                  tick[0].volume    = StringToInteger(result[5]);
                  tick[0].time  = StringToInteger(result[7]) - TimeGMTOffset();
                  tick[0].time_msc      = tick[0].time * 1000;
   
                  tick[0].flags     = TICK_FLAG_BID | TICK_FLAG_ASK | TICK_FLAG_LAST | TICK_FLAG_SELL;
   
                  Print("Local time:", TimeLocal(), ", Symbol:", symbol, ", Conid:", conid);
                  Print("Quote | price:", tick[0].last, ", volume:", tick[0].volume, ", Bar time:", tick[0].time);
   
                  
                  int ret = CustomTicksAdd(symbol, tick);
                  if (ret != 1) {
                     Print("更新报价错误码 : '", ret, "' 错误信息:'", GetLastError(), "'");
                  }
               } else {
                  rate[0].open      = StringToDouble(result[3]);
                  rate[0].high      = StringToDouble(result[4]);
                  rate[0].low       = StringToDouble(result[5]);
                  rate[0].close     = StringToDouble(result[6]);
                  rate[0].tick_volume = StringToInteger(result[7]);
                  rate[0].time      = StringToInteger(result[8])- TimeGMTOffset();
   
                  Print("Local time:", TimeLocal(), ", Symbol:", symbol, ", Conid:", conid);
                  Print("Quote | high:", rate[0].high, ", low:", rate[0].low, "open:", rate[0].open, ", close:", rate[0].close, ", volume:", rate[0].tick_volume, ", Bar time:", rate[0].time);
   
                  
                  int ret = CustomRatesUpdate(symbol, rate);
                  if (ret != 1) {
                     Print("更新报价错误码 : '", ret, "' 错误信息:'", GetLastError(), "'");
                  }
               }

            } else {

               Sleep(5000);
               return;
            }
         }
      } else {
        Print("Socket 无效,可能已被意外中断,请重启Script");
        return;
      }
   
  }


int RegisterInstrument(string symbols) {

// symbols holds the contract ids of all desired instruments, in digital form

   string url = RegURL + "?instrument=" + symbols + "&port=" + Port + "&action=register&type=IB";
   string cookie=NULL,headers;
   char post[],result[];
   int res;
   
   ResetLastError();
   
   int timeout=50000; 
   
   res=WebRequest("GET", url, cookie, NULL, timeout, post, 0, result, headers);
   
   if(res==-1) {
      Print("Error in WebRequest. Error code  =",GetLastError());
      return -1;
   }
   
   CJAVal js(NULL, jtUNDEF);
   PrintFormat("The server answers: \"%s\"", CharArrayToString(result));
   
   js.Deserialize(result);
   PrintFormat("res: \"%s\", instrument: \"%s\", port: \"%s\"", js["res"].ToStr(), js["instrument"].ToStr(), js["port"].ToStr());
   
   if (js["res"].ToStr() != "ok") {
      return -1;
   }
/*   
   if (js["instrument"].ToStr() != symbols) {
      return -2;
   }
   */
     
   return 0;
}