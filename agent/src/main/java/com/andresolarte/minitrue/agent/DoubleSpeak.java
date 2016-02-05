package com.andresolarte.minitrue.agent;

public class DoubleSpeak {
    public static int add(int x,int y) {
        int ret=x+y;
        if (ret>3) {
            ret++;
        }
        return ret;
    }

    public static String memoryHoleString(String s) {
        return s.replaceAll("4", "5");
    }
}
