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
                    console.log("DATA:");
                    for(row of dataSplit){
                        console.log(row);
                        // stompClient.send("/app/csvfile", {},
                        //                    JSON.stringify(row));
                    }
                }

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

                // while(readLine){
                //     $.ajax({
                //         type: "POST",
                //         url: "/csv",
                //         data: new FormData(this),
                //         enctype: 'multipart/form-data',
                //         processData: false,
                //         contentType: false,
                //         cache: false,
                //         success: function(msg) {
                //             console.log("Content recibido en cliente: " + msg.buffer);
                //             var blob = new Blob([msg.buffer], { type: 'text/csv' });
                //                 const url = window.URL.createObjectURL(blob);
                //                 const a = document.createElement('a');
                //                 a.style.display = 'none';
                //                 a.href = url;
                //                 // the filename you want
                //                 a.download = 'shortenedURLs.csv';
                //                 document.body.appendChild(a);
                //                 a.click();
                //                 window.URL.revokeObjectURL(url);
                //             $("#result").html(
                //                 "<div class='alert alert-success lead'>Generated CSV in root folder! :)</div>");
                //         },
                //         error: function(msg,error) {
                //          console.log(msg);
                //          console.log(error);
                //          console.log(error.body);
                //             $("#result").html(
                //                 "<div class='alert alert-danger lead'>" + msg.responseText +"</div>");
                //         }
                //     });
                // }

            });
    });