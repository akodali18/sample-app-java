package com.wfsample;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ShirtOrderGenerator {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        int numloops = Integer.parseInt(args[1]);
        int rampUpSeconds = Integer.parseInt(args[2]);
        int sleepSeconds = Integer.parseInt(args[3]);
        int affectedLoops = Integer.parseInt(args[4]);
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(2, TimeUnit.MINUTES).build();
        ExecutorService es = Executors.newFixedThreadPool(18);
        final AtomicBoolean pauseCreator = new AtomicBoolean(false);
        final int max = numloops * 2;
        for (int i = 0; i < numloops; i++) {
            final int j = 1 + i * 2;
            long sleepTime = rampUpSeconds * 1000;
            System.out.println("Trigger: " + i);
            try {
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
            for  (int l = 0; l < 3; l++) {
                es.submit(() -> {
                    int count = 0;
                    boolean slept = false;
                    while (true) {
                        int k = j;
                        if ( k > 6 && pauseCreator.get()) {
                            System.out.println("trigger3");
                            Thread.sleep((sleepSeconds * 1000));
                            slept =  true;
                        }
                        try {
                            RequestBody json = RequestBody.create(JSON, "{\"styleName\" : \"foo\",\"quantity\" : " +
                                    "" + k + "}");
                            Request.Builder requestBuilder = new Request.Builder().url
                                    ("http://35.236.55.1:" + port + "/shop/order").post(json);
                            Request request = requestBuilder.build();
                            Response response = client.newCall(request).execute();
                            if (response.code() != 200) {
                                System.out.println("error ordering shirts");
                            } else {
                                System.out.println("SUCCESS :: /shop/order");
                            }
                            response.close();
                        } catch (Exception e) {
                            System.out.println("error ordering shirts");
                        }
                        count++;
                        if( k >= 10 && count % affectedLoops == 0 && !slept) {
                            System.out.println("trigger 2");
                            pauseCreator.set(true);
                            Thread.sleep((sleepSeconds * 1000) / (max - k));
                            pauseCreator.set(false);
                        }
                        slept = false;
                        //System.out.println(k);


                        try {
                            HttpUrl url = new HttpUrl.Builder().scheme("http").host("35.236.55.1").port(port).
                                    addPathSegments("shop/menu")
                                    .build();
                            Request.Builder requestBuilder = new Request.Builder().url(url);
                            Request request = requestBuilder.build();
                            Response response = client.newCall(request).execute();
                            if (response.code() != 200) {
                                //throw new RuntimeException("Bad HTTP result: " + response);
                                System.out.println("error getting shopping menu");
                            } else {
                                System.out.println("SUCCESS :: /shop/menu");
                            }
                            response.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//          Thread.sleep(500);
                    }
                });
            }
        }
    }
}
