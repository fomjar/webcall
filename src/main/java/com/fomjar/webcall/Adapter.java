package com.fomjar.webcall;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import java.util.HashMap;
import java.util.Map;

public class Adapter implements
        AuthorizationListener,
        ConnectListener,
        DisconnectListener {

    @Override
    public boolean isAuthorized(HandshakeData data) {return true;}

    @Override
    public void onConnect(SocketIOClient client) {
        String user = client.getHandshakeData().getSingleUrlParam("user");
        String pass = client.getHandshakeData().getSingleUrlParam("pass");
        Caller caller = Caller.add(client, user, pass);

        Map data = new HashMap();
        data.put("pass", pass);
        this.onLink(caller, data);
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        Caller caller = Caller.del(client);
        if (null == caller) return;

        caller.client().getAllRooms().forEach(room -> {
            Map data = new HashMap();
            data.put("room", room);
            Adapter.this.onHang(caller, data);
        });
    }

    void setupEventListeners(SocketIOServer server) {
//        server.addEventListener(WebCall.EVENT_LINK, Map.class, (SocketIOClient client, Map data, AckRequest ackSender) -> Adapter.this.onLink(Caller.get(client), data));
        server.addEventListener(WebCall.EVENT_JOIN, Map.class, (SocketIOClient client, Map data, AckRequest ackSender) -> ackSender.sendAckData(Adapter.this.onJoin(Caller.get(client), data)));
        server.addEventListener(WebCall.EVENT_LEAV, Map.class, (SocketIOClient client, Map data, AckRequest ackSender) -> ackSender.sendAckData(Adapter.this.onLeav(Caller.get(client), data)));
        server.addEventListener(WebCall.EVENT_CALL, Map.class, (SocketIOClient client, Map data, AckRequest ackSender) -> ackSender.sendAckData(Adapter.this.onCall(Caller.get(client), data)));
        server.addEventListener(WebCall.EVENT_RING, Map.class, (SocketIOClient client, Map data, AckRequest ackSender) -> ackSender.sendAckData(Adapter.this.onRing(Caller.get(client), data)));
        server.addEventListener(WebCall.EVENT_PICK, Map.class, (SocketIOClient client, Map data, AckRequest ackSender) -> ackSender.sendAckData(Adapter.this.onPick(Caller.get(client), data)));
        server.addEventListener(WebCall.EVENT_HANG, Map.class, (SocketIOClient client, Map data, AckRequest ackSender) -> {ackSender.sendAckData(WebCall.RESULT_DONE); Adapter.this.onHang(Caller.get(client), data);});
        server.addEventListener(WebCall.EVENT_DATA, Map.class, (SocketIOClient client, Map data, AckRequest ackSender) -> ackSender.sendAckData(Adapter.this.onData(Caller.get(client), data)));
    }

    protected void   onLink(Caller caller, Map data) {caller.onLink(data);}
    protected String onJoin(Caller caller, Map data) {caller.onJoin(data); return WebCall.RESULT_DONE;}
    protected String onLeav(Caller caller, Map data) {caller.onLeav(data); return WebCall.RESULT_DONE;}
    protected String onCall(Caller caller, Map data) {caller.onCall(data); return WebCall.RESULT_DONE;}
    protected String onRing(Caller caller, Map data) {caller.onRing(data); return WebCall.RESULT_DONE;}
    protected String onPick(Caller caller, Map data) {caller.onPick(data); return WebCall.RESULT_DONE;}
    protected void   onHang(Caller caller, Map data) {caller.onHang(data);}
    protected String onData(Caller caller, Map data) {caller.onData(data); return WebCall.RESULT_DONE;}

}
