$(document).ready(
    function() {
        $("#shortener").submit(
            function(event) {
                event.preventDefault();
                $.ajax({
                    type: "POST",
                    url: "/link",
                    data: $(this).serialize(),
                    success: function(msg) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='" +
                            msg.uri +
                            "'>" +
                            msg.uri +
                            "</a></div>");
                        // Generate the link that would beused to generate the QR Code
                        let finalURL = 'https://chart.googleapis.com/chart?cht=qr&chl=' + msg.uri +
                            '&chs=200x200&chld=L|0'

                        // Replace the src of the image with the QR code
                        $('.qr-code').attr('src', finalURL);
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
                $.ajax({
                    type: "POST",
                    url: "/csv",
                    data: new FormData(this),
                    enctype: 'multipart/form-data',
                    processData: false,
                    contentType: false,
                    cache: false,
                    success: function(msg) {
                        console.log(msg);
                        var blob = new Blob([msg], { type: 'text/csv' });
                                            const url = window.URL.createObjectURL(blob);
                                            const a = document.createElement('a');
                                            a.style.display = 'none';
                                            a.href = url;
                                            // the filename you want
                                            a.download = 'shortenedURLs.csv';
                                            document.body.appendChild(a);
                                            a.click();
                                            window.URL.revokeObjectURL(url);
                        $("#result").html(
                            "<div class='alert alert-success lead'>Generated CSV in root folder! :)</div>");
                    },
                    error: function(msg,error) {
                     console.log(msg);
                     console.log(error);
                     console.log(error.body);
                        $("#result").html(
                            "<div class='alert alert-danger lead'>" + msg.responseText +"</div>");
                    }
                });
            });
    });