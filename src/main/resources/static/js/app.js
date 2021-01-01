$(document).ready(
    function() {
        $("#shortener").submit(
            function(event) {
                event.preventDefault();
                $.ajax({
                    type: "POST",
                    url: "/link",
                    cache: true,
                    data: $(this).serialize(),
                    success: function(msg) {
                        console.log(msg)
                        render = "";
                        if (msg.qrUrl != null) {
                            render ="<div class='alert alert-success lead'><a target='_blank' href='" +
                            msg.uri +
                            "'>" +
                            msg.uri +
                            "</a></div><img src='"+ msg.qrUrl +"' class='qr-code img-thumbnail img-responsive' />";
                        }
                        else{
                            render="<div class='alert alert-success lead'><a target='_blank' href='" +
                            msg.uri +
                            "'>" +
                            msg.uri +
                            "</a></div>";
                        }
                        $("#result").html(render);
                    },
                    error: function() {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });

        $("#csv").submit(
            function(event) {
                event.preventDefault();

                //***************WEBSOCKET***************

                // connect();
                var stompClient = null;
                var msgReceived = 0;
                var msgToReceive = 0;

                function connect() {
                    var socket = new SockJS('/ws-uploadCSV');
                    socket.binaryType = "arraybuffer";
                    stompClient = Stomp.over(socket);
                    console.log("Connecting STOMP ...");
                    stompClient.connect({}, function (frame) {
                        console.log('Connected: ' + frame);
                        stompClient.subscribe('/user/topic/getCSV',callback);
                    });
                }

                // Print received messages from the server
                callback =  function (msg) {
                    if (msg.body){
                    console.log("Message from server: " + msg.body);
                    console.log("msgToReceive: " + msgToReceive);
                    console.log("msgReceived: " + msgReceived);

                    // Add the message content to the csvArray object
                    // The message is converted to array
                    let temp = msg.body;
                    var processed = document.querySelector('.files');
                    // This will return an array with strings "1", "2", etc.
                    temp = temp.split(",");
                    // Update array content
                    generatedCsvContent.push(temp);
                    // Check if all messages has been received
                    if (msgReceived == msgToReceive){
                        processed.setAttribute('data-before', 'Upload complete!');
                        msgReceived = 0;
                        msgToReceive = 0;
                        download();
                    }
                    else{
                        //Shows on screen the % processed
                        var percent_done = Math.floor( ( msgReceived / msgToReceive ) * 100 );
                        processed.setAttribute('data-before', `Proccesing File -  ${percent_done}% ...`);
                        msgReceived ++;
                    }
                    }else{
                    console.log("Empty msg.");
                    }
                }

                connect();
                sleep(1000).then(() =>{
                    sendFileWS()
                });

                function sendFileWS(){
                    //Reading file and sending petitions to server
                    var reader = new FileReader();
                    var file = document.getElementById('file').files[0]
                    reader.readAsArrayBuffer(file);
                    reader.onloadend = function(ev){
                        // Get the Array Buffer
                        let data = ev.target.result;
                        let ui8a = new Uint8Array(data, 0);
                        // Grab our byte length
                        data = String.fromCharCode.apply(null,ui8a);
                        // Now we have the file as String
                        console.log("DATA:" + data);
                        var dataSplit = data.split(/\n/);
                        // dataSplit = dataSplit.shift();
                        console.log("DATA:" + dataSplit);
                        // Delete header "URI"
                        dataSplit.shift();
                        for(row of dataSplit){
                            console.log(row);
                            stompClient.send("/app/uploadCSV", {}, lines);

                            // Send row to server
                            // stompClient.send("/app/csv", {},
                            //                 JSON.stringify(row));
                        }
                    }
                }


                // function connect(){
                //     //Create webSocket
                //     var socket = new SockJS('/csv-websocket');
                //     stompClient = Stomp.over(socket); 
                //     stompClient.connect({}, function(frame) {
                //         console.log('Connected: ' + frame);
                //         stompClient.subscribe('/topic/messages', function(messageOutput) {
                //             alert(messageOutput);
                //             console.log("MessageOutput: "+ messageOutput);
                //             console.log(JSON.parse(messageOutput.body));
                //         });
                //     }); 
                // }
                

                // function disconnect() {
                //     if (stompClient !== null) {
                //         stompClient.disconnect();
                //     }
                //     console.log("Disconnected");
                // }

                // var reader = new FileReader();
                // var file = document.getElementById('file').files[0]
                // reader.readAsArrayBuffer(file);
                // reader.onloadend = function (evt) {
                //     // Get the Array Buffer
                //     var data = evt.target.result;
                //     var ui8a = new Uint8Array(data, 0);
                //     // Grab our byte length
                //     data = String.fromCharCode.apply(null,ui8a);
                //     number = data.split(/\r\n|\r|\n/).length;
                //     var rows = data.split('\n');
                //     console.log("DATA:");
                //     for(var i=0; i<number; i++){
                //         console.log(rows);
                //         stompClient.send("/app/csvfile", {},
                //                            JSON.stringify(rows[i]));
                //     }
                //  }
            });
    });