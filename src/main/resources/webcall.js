
function WebCall() {

    this.socket = null;
    this.url    = null;
    this.user   = null;
    this.pass   = null;
    this.room   = null;
    this.slice  = 2000;
    this.redun  = 300;
    this.timer_record   = null;
    this.timer_sender   = null;
    this.timer_player   = null;
    this.players        = null;
    this.queue_data     = [];
    this.events = {
        'init'  : [],
        'link'  : [],
        'join'  : [],
        'leav'  : [],
        'call'  : [],
        'ring'  : [],
        'pick'  : [],
        'talk'  : [],
        'hang'  : [],
        'data'  : []
    };

    /**
     * register event listener.
     * available events:
     * init, link, join, call, ring, pick, talk, hang, data
     *
     * @param event
     * @param listener
     *
     */
    this.addevent = function(event, listener) {
        if (!this.events[event]) return;

        this.events[event].push(listener);
    };

    /**
     * dispatch event.
     *
     * @param event
     * @param room
     * @param user
     *
     */
    this.dispatch = function(event, data) {
        for (var i in this.events[event]) {
            var l = this.events[event][i];
            l.call(this, data);
        }
    };

    /**
     * try to connect to server.
     *
     * @param url
     * @param user
     * @param pass
     * @param done
     */
    this.link = function(url, user, pass, done) {

        if (url)    this.url    = url;
        if (user)   this.user   = user;
        if (pass)   this.pass   = pass;

        this.socket = io(this.url + '/?user='+ this.user + "&pass=" + this.pass);

        var that = this;
        this.socket.on('disconnect', function(data) {
            console.log('[init] room(' + that.room + ') user(' + that.user + ')');
            that.dispatch('init', that.room, that.user);
        });
        this.socket.on('connect',    function() {});
        this.socket.on('webcall-link', function(data) {
            console.log('[link] user(' + data.user + ')');
            that.dispatch('link', data);
        });
        this.socket.on('webcall-join', function(data) {
            console.log('[join] room(' + data.room + ') user(' + data.user + ')');
            that.dispatch('join', data);
        });
        this.socket.on('webcall-leav', function(data) {
            console.log('[leav] room(' + data.room + ') user(' + data.user + ')');
            that.dispatch('leav', data);
        });
        this.socket.on('webcall-call', function(data) {
            console.log('[call] room(' + data.room + ') user(' + data.user + ')');
            that.dispatch('call', data);
        });
        this.socket.on('webcall-ring', function(data) {
            console.log('[ring] room(' + data.room + ') user(' + data.user + ')');
            that.dispatch('ring', data);
        });
        this.socket.on('webcall-pick', function(data) {
            console.log('[pick] room(' + data.room + ') user(' + data.user + ')');
            that.dispatch('pick', data);
        });
        this.socket.on('webcall-talk', function(data) {
            console.log('[talk] room(' + data.room + ') user(' + data.user + ')');
            that.dispatch('talk', data);
        });
        this.socket.on('webcall-hang', function(data) {
            console.log('[hang] room(' + data.room + ') user(' + data.user + ')');
            that.dispatch('hang', data);
        });
        this.socket.on('webcall-data', function(data) {
            console.log('[data] room(' + data.room + ') user(' + data.user + ') at (' + data.time + ')');
            that.dispatch('data', data);
        });

        if (done) done();
    };

    /**
     * try to join the room.
     * @param room
     * @param done
     */
    this.join = function(room, done) {this.socket.emit('webcall-join', {room: room}, done)};
    /**
     * try to leave the room.
     * @param room
     * @param done
     */
    this.leav = function(room, done) {this.socket.emit('webcall-leav', {room: room}, done)};
    /**
     * try to send a call request in the room.
     * @param room
     * @param done
     */
    this.call = function(room, done) {this.socket.emit('webcall-call', {room: room}, done)};
    /**
     * try to send a ring event to the others in the room.
     * @param room
     * @param done
     */
    this.ring = function(room, done) {this.socket.emit('webcall-ring', {room: room}, done)};
    /**
     * try to pick up someones call in the room.
     * @param room
     * @param done
     */
    this.pick = function(room, done) {this.socket.emit('webcall-pick', {room: room}, done)};
    /**
     * try to start talking in the room.
     * @param room
     * @param done
     */
    this.talk = function(room, done) {this.socket.emit('webcall-talk', {room: room}, done)};
    /**
     * try to hang up this call in the room.
     * @param room
     * @param done
     */
    this.hang = function(room, done) {this.socket.emit('webcall-hang', {room: room}, done)};
    /**
     * try to broad my data to the others in the room.
     * @param data
     * @param done
     */
    this.data = function(data, done) {this.socket.emit('webcall-data', data, done)};

    this.addevent('init', function(data) {this.dispatch('hang', data)});
    this.addevent('join', function(data) {this.room = data.room;});
    this.addevent('call', function(data) {
        if (this.user == data.user) return;

        this.ring(data.room);
    });
    this.addevent('hang', function(data) {
        if (this.user != data.user) return;

        if (this.timer_record) {
            window.clearInterval(this.timer_record);
            this.timer_record = null;
        }
        if (this.timer_sender) {
            window.clearInterval(this.timer_sender);
            this.timer_sender = null;
        }
        if (this.timer_player) {
            window.clearInterval(this.timer_player);
            this.timer_player = null;
        }
        this.socket.close();
        this.queue_data = [];
    });


    this.addevent('talk', function(data) {
        if (data.user != this.user)     return;
        if (null != this.timer_record)  return; // talking already

        var that = this;
        var room = data.room;
        navigator.mediaDevices.getUserMedia({audio: true})
            .then(function(stream) {
                that.timer_record = window.setInterval(function() {
                    var recorder = RecordRTC(stream, {
                        recorderType: StereoAudioRecorder,
                        mimeType    : 'audio/wav',
                        type        : 'audio',
                    });
                    window.setTimeout(function() {
                        recorder.stopRecording(function() {
                            recorder.getDataURL(function(data) {
                                that.queue_data.push({
                                    time    : new Date().getTime(),
                                    room    : room,
                                    data    : data,
                                });
                            });
                        });
                    }, that.slice + that.redun);
                    recorder.startRecording();
                }, that.slice);
            });
        that.timer_sender = window.setInterval(function() {
            var data = null;
            while (data = that.queue_data.shift()) {
                that.data(data);
            }
        }, 10);
        that.timer_player = window.setInterval(function() {
            if (null == that.players)                   return;
            if (0 == that.players.childElementCount)    return;

            if (1 == that.players.childElementCount) {
                if (that.players.firstChild.currentTime <= 0.1)
                    that.players.firstChild.play();
                return;
            }

            for (var i in that.players.childNodes) {
                if (i != that.players.childElementCount - 1) {
                    var player = that.players.childNodes[i];
                    if (player.currentTime < 0.1) {
                        player.play();
                        player.addEventListener("canplaythrough", function() {
                            player.play();
                        });
                    }
                }
            }

            var curr = that.players.childNodes[that.players.childElementCount - 2];
            var delta = that.slice - curr.currentTime * 1000;
            if (delta - that.redun * 1.5 <= 5) {
                var next = that.players.childNodes[that.players.childElementCount - 1];
                if (next) {
                    next.play();
                    next.addEventListener("canplaythrough", function() {
                        next.play();
                    });
                }
            }
        }, 5);
    });
    this.addevent('data', function(data) {
        if (data.user == this.user) return;
        if (null == this.players)   return;

        var audio = document.createElement("audio");
        audio.autoplay = false;
        audio.controls = true;
        audio.src = data.data;
        this.players.appendChild(audio);
        audio.addEventListener("ended", function() {
            audio.remove();
        });
    })
}
