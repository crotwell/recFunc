#!/usr/bin/perl

while(<>) {
   s/km\/s//g;
   s/km//g;
   s/ //g;
   s/,/ /g;
   print;
}
