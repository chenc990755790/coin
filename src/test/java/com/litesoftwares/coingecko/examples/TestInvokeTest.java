package com.litesoftwares.coingecko.examples;

import java.lang.reflect.Method;
import java.util.List;

public class TestInvokeTest {

//    public void test1(String key, List<String> stringList) {
//        System.out.println(key + " :" + stringList);
//    }

    public  void test2() throws NoSuchMethodException {
        Method test1 = this.getClass().getMethod("test1", String.class, List.class);
        System.out.println(test1);
    }

    public static void main(String[] args) throws NoSuchMethodException {
        TestInvokeTest test = new TestInvokeTest();
        test.test2();
    }
}
