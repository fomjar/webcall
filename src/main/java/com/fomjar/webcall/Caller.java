package com.fomjar.webcall;

import com.corundumstudio.socketio.SocketIOClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Caller {

    private static final Logger logger = LoggerFactory.getLogger(Adapter.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static Map<String, Map<SocketIOClient, Caller>> callers = null;

    static synchronized Caller add(SocketIOClient client, String user, String pass) {
        if (null == Caller.callers)
            Caller.callers = new LinkedHashMap<>();
        if (!Caller.has(user))
            Caller.callers.put(user, new LinkedHashMap<>());

        Caller caller = Caller.get(client);
        if (null != caller) return caller;

        caller = new Caller(client, user, pass);
        Caller.get(user).put(client, caller);

        return caller;
    }

    public static Map<SocketIOClient, Caller> getAll() {
        return Caller.callers.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    public static boolean has(String user) {
        if (null == Caller.callers) return false;

        return Caller.callers.containsKey(user);
    }

    public static boolean has(SocketIOClient client) {
        if (null == Caller.callers) return false;

        return getAll().containsKey(client);
    }

    public static Map<SocketIOClient, Caller> get(String user) {
        if (null == Caller.callers) return null;

        return Caller.callers.get(user);
    }

    public static Caller get(SocketIOClient client) {
        if (null == Caller.callers) return null;

        return getAll().get(client);
    }

    public static synchronized Caller del(SocketIOClient client) {
        if (null == Caller.callers) return null;

        Caller caller = Caller.get(client);
        if (null == caller) return null;

        Map callers = Caller.callers.get(caller.user());
        callers.remove(client);
        if (callers.isEmpty()) Caller.callers.remove(caller.user());

        return caller;
    }


    private SocketIOClient  client;
    private String          user;
    private String          pass;

    private Caller(SocketIOClient client, String user, String pass) {
        this.client = client;
        this.user   = user;
        this.pass   = pass;
        client.leaveRoom("");
    }

    public SocketIOClient   client()    {return this.client;}
    public String   user()  {return this.user;}
    public String   pass()  {return this.pass;}

    void onLink(Map data) {
        data.put("user", this.user());
        this.client().sendEvent(WebCall.EVENT_LINK, data);
        logger.info(String.format("user(%s) link", this.user()));
    }

    void onJoin(Map data) {
        data.put("user", this.user());
        String room = data.get("room").toString();
        this.client().joinRoom(room);
        this.client().getNamespace().getRoomOperations(room).sendEvent(WebCall.EVENT_JOIN, data);
        logger.info(String.format("user(%s) join room(%s)", this.user(), room));
    }

    void onLeav(Map data) {
        data.put("user", this.user());
        String room = data.get("room").toString();
        this.client().leaveRoom(room);
        this.client().getNamespace().getRoomOperations(room).sendEvent(WebCall.EVENT_LEAV, data);
        logger.info(String.format("user(%s) leav room(%s)", this.user(), room));
    }

    void onCall(Map data) {
        data.put("user", this.user());
        String room = data.get("room").toString();
        this.client().getNamespace().getRoomOperations(room).sendEvent(WebCall.EVENT_CALL, data);
        logger.info(String.format("user(%s) call room(%s)", this.user(), room));
        this.client().sendEvent(WebCall.EVENT_TALK, data);
        logger.info(String.format("user(%s) talk room(%s)", this.user(), room));
    }

    void onRing(Map data) {
        data.put("user", this.user());
        String room = data.get("room").toString();
        this.client().getNamespace().getRoomOperations(room).sendEvent(WebCall.EVENT_RING, this.client(), data);
        logger.info(String.format("user(-%s) ring room(%s)", this.user(), room));
    }

    void onPick(Map data) {
        data.put("user", this.user());
        String room = data.get("room").toString();
        this.client().getNamespace().getRoomOperations(room).sendEvent(WebCall.EVENT_PICK, data);
        logger.info(String.format("user(%s) pick room(%s)", this.user(), room));
        this.client().sendEvent(WebCall.EVENT_TALK, data);
        logger.info(String.format("user(%s) pick room(%s)", this.user(), room));
    }

    void onHang(Map data) {
        data.put("user", this.user());
        String room = data.get("room").toString();
        this.client().getNamespace().getRoomOperations(room).sendEvent(WebCall.EVENT_HANG, data);
        this.client().leaveRoom(room);
        logger.info(String.format("user(%s) hang room(%s)", this.user(), room));
    }

    void onData(Map data) {
        data.put("user", this.user());
        String room = data.get("room").toString();
        this.client().getNamespace().getRoomOperations(room).sendEvent(WebCall.EVENT_DATA, this.client(), data);
        logger.info(String.format("user(%s) data room(%s) at (%s)",
                this.user(),
                room,
                sdf.format(Long.valueOf(data.get("time").toString()))));
    }

}
