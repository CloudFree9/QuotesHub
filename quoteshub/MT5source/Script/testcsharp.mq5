//+------------------------------------------------------------------+
//|                                                   testcsharp.mq5 |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2018, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+

#import "Win32Project1.dll"
                    
int  Add(       int    left, int    right );
int  Sub(       int    left, int    right );
#import


#property strict                 // MQL-syntax-mode-modifier == "strict"

    
void OnStart()
{   int k = 0;
    MessageBox( k );             // this call works
    k = Add( 1, 666 );
    MessageBox( k );             // Doesn't work

    }
