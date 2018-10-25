package com.fomjar.webcall;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

public class WebCall {

    public static final String EVENT_LINK   = "webcall-link";
    public static final String EVENT_JOIN   = "webcall-join";
    public static final String EVENT_LEAV   = "webcall-leav";
    public static final String EVENT_CALL   = "webcall-call";
    public static final String EVENT_RING   = "webcall-ring";
    public static final String EVENT_PICK   = "webcall-pick";
    public static final String EVENT_TALK   = "webcall-talk";
    public static final String EVENT_HANG   = "webcall-hang";
    public static final String EVENT_DATA   = "webcall-data";

    public static final String RESULT_DONE  = "webcall-done";
    public static final String RESULT_FAIL  = "webcall-fail";


    public static WebCall start(Integer port) {
        return WebCall.start(null, port, null, null);
    }

    public static WebCall start(Integer port, Adapter adapter) {
        return WebCall.start(null, port, null, adapter);
    }

    public static WebCall start(Integer port, Integer pool) {
        return WebCall.start(null, port, pool, null);
    }

    public static WebCall start(Integer port, Integer pool, Adapter adapter) {
        return WebCall.start(null, port, pool, adapter);
    }

    public static WebCall start(String host, Integer port, Integer pool) {
        return WebCall.start(host, port, pool, null);
    }

    public static WebCall start(String host, Integer port, Integer pool, Adapter adapter) {
        WebCall server = new WebCall(host, port, pool);
        if (null == adapter) adapter = new Adapter();
        server.start(adapter);
        return server;
    }

    private String  host;
    private Integer port;
    private Integer pool;
    private SocketIOServer server;

    private WebCall(String host, Integer port, Integer pool) {
        this.host = host;
        this.port = port;
        this.pool = pool;
        this.server = null;
    }

    private synchronized void start(Adapter adapter) {
        if (null != server) return;

        Configuration conf = new Configuration();
        if (null != this.host) conf.setHostname(this.host);
        if (null != this.port) conf.setPort(this.port);
        if (null != this.pool) conf.setWorkerThreads(this.pool);
        conf.setMaxFramePayloadLength(Integer.MAX_VALUE);
        conf.setMaxHttpContentLength(Integer.MAX_VALUE);
        conf.setOrigin(null);   // 支持跨域
        conf.setAuthorizationListener(adapter);

        this.server = new SocketIOServer(conf);
        this.server.addConnectListener(adapter);
        this.server.addDisconnectListener(adapter);
        adapter.setupEventListeners(this.server);

        this.server.start();
    }

    public synchronized void stop() {
        if (null == this.server) return;

        this.server.stop();
        this.server = null;
    }

}
