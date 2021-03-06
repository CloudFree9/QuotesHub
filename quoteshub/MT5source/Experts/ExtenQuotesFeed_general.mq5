//+------------------------------------------------------------------+
//|                                              ExtenQuotesFeed.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+


#property copyright "Copyright 2017, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"

#include "socketlib.mqh"
#include "JAson.mqh"
#include <Controls\Button.mqh>
#include <VirtualKeys.mqh>

//--- input parameters
string   underlying="IC1802";
input string   Host        = "0.0.0.0";
string   Title       = "中证1802";

input string   TradeAgent  = "127.0.0.1";
input string   TAPort      = "1273";

double         lastDeal    = 2689;
double         lastBid     = 2688;
double         lastAsk     = 2689;
ulong          lastVolume  = 1000;   
ulong          Port        = 1998;
bool ctrl_pressed = false;

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

void drawButton() {
   ResetLastError();
//--- create the button
int chart_ID = 0;
string name = "order_btn";

   if(!ObjectCreate(chart_ID,name,OBJ_BUTTON,0,0,0))
     {
      Print(__FUNCTION__,
            ": failed to create the button! Error code = ",GetLastError());
      return;
     }
//--- set button coordinates
   ObjectSetInteger(chart_ID,name,OBJPROP_XDISTANCE,320);
   ObjectSetInteger(chart_ID,name,OBJPROP_YDISTANCE,5);
//--- set button size
   ObjectSetInteger(chart_ID,name,OBJPROP_XSIZE,120);
   ObjectSetInteger(chart_ID,name,OBJPROP_YSIZE,25);
//--- set the chart's corner, relative to which point coordinates are defined
//   ObjectSetInteger(chart_ID,name,OBJPROP_CORNER,corner);
//--- set the text
   ObjectSetString(chart_ID,name,OBJPROP_TEXT,"点击下单(Ctrl+F2)");
//--- set text font
   ObjectSetString(chart_ID,name,OBJPROP_FONT,"宋体");
//--- set font size
   ObjectSetInteger(chart_ID,name,OBJPROP_FONTSIZE,8);
//--- set text color
   ObjectSetInteger(chart_ID,name,OBJPROP_COLOR,clrBlack);
//--- set background color
   ObjectSetInteger(chart_ID,name,OBJPROP_BGCOLOR,clrSilver);
//--- set border color
   ObjectSetInteger(chart_ID,name,OBJPROP_BORDER_COLOR,clrYellow);
//--- display in the foreground (false) or background (true)
   ObjectSetInteger(chart_ID,name,OBJPROP_BACK,false);
//--- set button state
   ObjectSetInteger(chart_ID,name,OBJPROP_STATE,false);
//--- enable (true) or disable (false) the mode of moving the button by mouse
   ObjectSetInteger(chart_ID,name,OBJPROP_SELECTABLE,true);
   ObjectSetInteger(chart_ID,name,OBJPROP_SELECTED,false);
//--- hide (true) or display (false) graphical object name in the object list
   ObjectSetInteger(chart_ID,name,OBJPROP_HIDDEN,false);
//--- set the priority for receiving the event of a mouse click in the chart
   ObjectSetInteger(chart_ID,name,OBJPROP_ZORDER,0);
   
 }

void drawTitle() {

   ResetLastError();
//--- create the button

   int chart_ID = 0;
   string name = "title";

   ObjectCreate(0,name,OBJ_LABEL,0,0,0);
   ObjectSetInteger(chart_ID,name,OBJPROP_XDISTANCE,130);
   ObjectSetInteger(chart_ID,name,OBJPROP_YDISTANCE,5);

   ObjectSetString(chart_ID,name,OBJPROP_TEXT,Title);
//--- set text font
   ObjectSetString(chart_ID,name,OBJPROP_FONT,"宋体");
//--- set font size
   ObjectSetInteger(chart_ID,name,OBJPROP_FONTSIZE,12);
//--- set text color
   ObjectSetInteger(chart_ID,name,OBJPROP_COLOR,clrYellow);
   ObjectSetInteger(chart_ID,name,OBJPROP_BGCOLOR,clrSilver);
   ObjectSetInteger(chart_ID,name,OBJPROP_BACK,false);

//   ObjectSetInteger(chart_ID,name,OBJPROP_STATE,false);
//--- enable (true) or disable (false) the mode of moving the button by mouse
   ObjectSetInteger(chart_ID,name,OBJPROP_SELECTABLE,false);
   ObjectSetInteger(chart_ID,name,OBJPROP_SELECTED,false);
//--- hide (true) or display (false) graphical object name in the object list
   ObjectSetInteger(chart_ID,name,OBJPROP_HIDDEN,true);
//--- set the priority for receiving the event of a mouse click in the chart
   ObjectSetInteger(chart_ID,name,OBJPROP_ZORDER,1);

/*

   ObjectCreate(0,Title,OBJ_BITMAP_LABEL,0,0,0);
//--- 指定用于在CLOCK物件编写的图形资源的名称
   ObjectSetString(0,Title,OBJPROP_BMPFILE,"::IMG");
   
   int w = 400, h = 20;
   uint ExtImg[400*20];
   ArrayFill(ExtImg , 0, 400*20, 0);
   TextSetFont("宋体",-96,FW_EXTRABOLD,0);
   
   TextOut(Title, 85, 5, TA_LEFT|TA_TOP, ExtImg, w, h, 0xFFFFFF00, COLOR_FORMAT_XRGB_NOALPHA);
 
   ResourceCreate("::IMG",ExtImg,w,h,0,0,w,COLOR_FORMAT_XRGB_NOALPHA);
      //--- 强制图表更新
  
   ChartRedraw();
   */
 }

int registerInstrument() {

//   string url = "http://" + TradeAgent + "/";

   string url = "http://" + TradeAgent + "/regquote?instrument=" + underlying + "&port=" + Port + "&action=register&type=CTP";
   string cookie=NULL,headers;
   char post[],result[];
   int res;
   
   ResetLastError();
   
   int timeout=50000; 
   
   res=WebRequest("GET", url, cookie, NULL, timeout, post, 0, result, headers);
   
   if(res==-1) {
      Print("Error in WebRequest. Error code  =",GetLastError());
      //--- 也许URL没有列出，显示需要添加地址的信息
//      MessageBox("Add the address '" + url + "' in the list of allowed URLs on tab 'Expert Advisors'","Error", MB_ICONINFORMATION);
      return -1;
   }
   
   CJAVal js(NULL, jtUNDEF);
   PrintFormat("The server answers: \"%s\"", CharArrayToString(result));
   
   js.Deserialize(result);
   PrintFormat("res: \"%s\", instrument: \"%s\", port: \"%s\"", js["res"].ToStr(), js["instrument"].ToStr(), js["port"].ToStr());
   
   if (js["res"].ToStr() != "ok") {
      return -1;
   }
   
   if (js["instrument"].ToStr() != underlying) {
      return -2;
   }
   
   Port = js["port"].ToInt();
   
   return 0;
}

void OnChartEvent(const int id,
                  const long &lparam,
                  const double &dparam,
                  const string &sparam)
  {

   

   switch(id) {
      case CHARTEVENT_KEYDOWN:
         if (ctrl_pressed == false && lparam == VK_CONTROL)
            ctrl_pressed = true;
         else if (ctrl_pressed == true) {
            ctrl_pressed = false;
            if (lparam == VK_F2) {
              orderForm();
            }
         }
         break;
      case CHARTEVENT_OBJECT_CLICK:
         if (sparam == "order_btn") {
            orderForm();
         }
   
   }
   
}
  
void orderForm() {
   string url = "http://" + TradeAgent + "/ctptrade/orderform?con=" + underlying;
   string cookie=NULL,headers;
   char post[],result[];
   int res;
   
   ResetLastError();
   
   int timeout=50000; 
   
   res=WebRequest("GET", url, cookie, NULL, timeout, post, 0, result, headers);
   
   if(res==-1) {
      Print("Error in WebRequest. Error code  =",GetLastError());
      return;
   }
   
   CJAVal js(NULL, jtUNDEF);
   PrintFormat("服务应答: \"%s\"", CharArrayToString(result));
   
   js.Deserialize(result);
   
   if (js["res"].ToStr() == "ok") {
      PrintFormat("开启订单页面命令已发出！");
   }
}
   
//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit() {

   underlying = _Symbol;
   Title = SymbolInfoString(_Symbol, SYMBOL_DESCRIPTION);
   
   char wsaData[]; 
   ArrayResize(wsaData, sizeof(WSAData));
   int res = WSAStartup(MAKEWORD(2,2), wsaData);
   if(res != 0) { 
      Print("错误 - WSAStartup 初始化失败: " + string(res)); 
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
   addrin.sin_port=htons(0);
   ref_sockaddr ref; 
   ref.in=addrin;
       
   if(bind(server,ref.ref,sizeof(addrin)) == SOCKET_ERROR) {
      int err=WSAGetLastError();
      if(err!=WSAEISCONN) { 
         Print("端口绑定失败: " + WSAErrorDescript(err) + ", 进行清理工作..."); 
         CloseClean(); 
         return INIT_FAILED; 
      }
   }
   
   int len = sizeof(addrin);
   if (getsockname(server, ref.ref, len) != 0){
      Print("端口绑定失败, 进行清理工作..."); 
      CloseClean(); 
      return INIT_FAILED; 
   }

   Port = ntohs(ref.in.sin_port);

   int block_mode = 1;
   res=ioctlsocket(server,(int)FIONBIO,block_mode);
         
   if(res != NO_ERROR) { 
      Print("ioctlsocket 失败: " + string(res) + ", 进行清理工作..."); 
      CloseClean(); 
      return INIT_FAILED; 
   }
   
   if (registerInstrument() != 0) {
      Print("错误 - 标的注册失败: " + underlying); 
      return INIT_FAILED;    
   }
 
   Print("本地服务启动成功！");
   
   EventSetTimer(1);
//---
   drawButton();
   drawTitle();

   ChartRedraw();

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
 //  ResourceFree("::IMG");
   Print("清理完成, Bye!"); 
   ObjectDelete(0, "order_btn");
   ObjectDelete(0, "title");
   ChartRedraw();
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
                  Print("更新报价错误码 : '", ret, "' 错误信息:'", GetLastError(), "'");
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
