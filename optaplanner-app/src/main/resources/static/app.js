var stompClient = null;
var socket = null;
var shortName = "";

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#events").show();
    }
    else {
        $("#events").hide();
    }
    $("#cacheEvents").html("");
}

function connect() {
    // create the SockJS WebSocket-like object
	socket = new SockJS('/optaplanner-app');
	
	// specify that we're using the STOMP protocol on the socket
    stompClient = Stomp.over(socket);
    
    // implement the behavior we want whenever the client connects to the server
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        
        // subscribe to topic and create the callback function that handles updates from the server
        stompClient.subscribe("/topic/damageEvents", function (event) {
            console.log(event);
            var body = JSON.parse(event.body);
            showEvent(body.type + ". Key: " + body.key);
        });

        sendName();
    });
}

function sendName() {
    stompClient.send("/app/cacheSubscribe", {}, JSON.stringify({'cacheName': $("#shortName").val()}));
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}


function showEvent(event) {
    $("#cacheEvents").append("<tr><td>" + event + "</td></tr>");
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    
    $( "#connect" ).click(function() { connect(); });
    
    $( "#disconnect" ).click(function() { disconnect(); });
});

