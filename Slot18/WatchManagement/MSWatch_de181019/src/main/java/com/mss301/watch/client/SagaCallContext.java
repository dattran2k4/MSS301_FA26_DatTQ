package com.mss301.watch.client;
public final class SagaCallContext {
 private static final ThreadLocal<String[]> CURRENT=new ThreadLocal<>();
 private SagaCallContext() {}
 public static void set(String id,String phase) { CURRENT.set(new String[]{id,phase}); }
 public static String[] get() { return CURRENT.get(); }
 public static void clear() { CURRENT.remove(); }
}
