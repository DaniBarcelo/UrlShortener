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
                var stompClient = null;
                var numMsgReceived = 0;
                var numMsgTotal = 0;
                var csvContent = ""; //Array to save all content received from server
            

                function connect() {
                    var socket = new SockJS('/websocketsCSV');
                    socket.binaryType = "arraybuffer";
                    stompClient = Stomp.over(socket);
                    console.log("Connecting STOMP ...");
                    stompClient.connect({}, function (frame) {
                        console.log('Connected: ' + frame);
                        stompClient.subscribe('/user/topic/websocket-csv-client',callback);
                    });
                }

                // Print received messages from the server 
                // Server returns message with format URL,SHORTED_URL 
                callback =  function (msg) {
                    if (msg.body){
                        console.log("Message from server: " + msg.body);
                        console.log("numMsgReceived: " + numMsgReceived);
                        
                        numMsgReceived++;              
                        csvContent += (msg.body);
                        // Check if all messages has been received
                        if (numMsgReceived == numMsgTotal){
                        console.log("TODOS MENSAJES RECIBIDOS");
                        
                            //DOWNLOAD FILE with content
                            //TODO
                            createFileAndDownload(csvContent);
                        }
                    }else{
                        console.log("No mesage");
                    }
                }

                // Source: https://www.sitepoint.com/delay-sleep-pause-wait/
                function sleep(ms) {
                    return new Promise(resolve => setTimeout(resolve, ms));
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
                        var dataSplit = data.split('\n');
                        number = data.split(/\r\n|\r|\n/).length;
                        console.log("DATA:" + dataSplit);
                        // Delete header "URI"
                        dataSplit.shift();
                        numMsgTotal = dataSplit.length;
                        console.log("numMensagesToReceive: " + numMsgTotal);
                        for(row of dataSplit){
                            console.log("Row: " + row);
                            stompClient.send("/app/websocket-csv-server", {}, row);
                        }
                    }
                }

                //Creates csv file with data in parameter "body"
                function createFileAndDownload(body){
                    var headersCSV = "url,shortened URL\n";
                    stompClient.disconnect();
                    console.log("Content recibido en cliente: " + body);
                    var blob = new Blob([headersCSV + body], { type: 'text/csv' });
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.style.display = 'none';
                    a.href = url;
                    // the filename you want
                    a.download = 'shortenedURLs.csv';
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);

                }
                

                function disconnect() {
                    if (stompClient !== null) {
                        stompClient.disconnect();
                    }
                    console.log("Disconnected");
                }
            });
    });