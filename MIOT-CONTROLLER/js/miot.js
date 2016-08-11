
//what to do when enter key is pressed on the form
function processForm(){

	var text = $("#chatto").val();


  if(myName === false){
    myName = text;
  }

  //sending the message through websoket
  sendDataWS('message', text);


	//emptying input
	$("#chatto").val('');
	$("#chatto").focus();

}

// writing to logbook
function writeLogbook(message){
	
	$("#logbook").append("<br/>"+ message); //should be changed to append()

  //scroll to bottom of logbook
  $("#logbook").scrollTop($("#logbook").prop("scrollHeight"));

}

// changing value of a field
function changeValue(theid, message){
  $(theid).html(message);  
}

//checking which keys is pressed, and how to do with it.
function whichkeys(event,writehere){
  var keyvalue;
	if(event.which == 38){
        keyvalue = "UP";
        changeValue(writehere,"UP");
      }
      else if(event.which == 40){
        keyvalue = "DOWN";
        changeValue(writehere,"DOWN");
      }
      else if(event.which == 39){
        changeValue(writehere,"RIGHT");
      }
      else if(event.which == 37){
        changeValue(writehere,"LEFT");
      }
      else if(event.which == 27){
        changeValue(writehere,"ESC");
      }
      else if(event.which == 32){
        changeValue(writehere,"SPACE");
      }
      else if(event.which == 17){
        changeValue(writehere,"CTRL");
      }
      else if(event.which == 18){
        changeValue(writehere,"ALT");
      }
      else if(event.which == 16){
        changeValue(writehere,"SHIFT");
      }
      else if(event.which == 13){
        changeValue(writehere,"ENTER");
      }
      else{
        changeValue(writehere,String.fromCharCode(event.which));  
      }

}

      //============ THIS IS TO TEST RANDOMIZED INTERVAL
      $("#randomstart").click(function() {

          $('#randomstart').hide(500);
          $('#randomstop').show(500);

          globalinterval = setInterval(function(){
                              //generating random part
                              var random = Math.floor((Math.random() * 100) + 1);

                              sendDataWS('message', random);


                              var text = "::: Generating Random Number : " + random;          
                              //write in logbook
                              writeLogbook(text);

                              changeValue("#userkey", random);                    
                              
                          }, 1000); 
          
      });

      $("#randomstop").click(function() {

          clearInterval(globalinterval);
          var text = "::: Stopping random number";
          writeLogbook(text);          
          
          $('#randomstart').show(500);
          $('#randomstop').hide(500);

      });      



// ===========================================================================================
//    MIOT FRAMEWORK CLIENT
//    This is a client framework in which you can connect to a MIOT server, 
//    AUTHOR : Toni Tegar Sahidi [tonitegarsahidi @ gmail .com]
// ===========================================================================================


//***************************************
// Connect to WEBSOCKET SERVER
//***************************************
function connectWS(){
    "use strict";

    // for better performance - to avoid searching in DOM
    var content = $('#content');
    var input = $('#chatto');
    var status = $('#logbook');


    // if browser doesn't support WebSocket, just show some notification and exit
    if (!window.WebSocket) {
        writeLogbook("OOOOOPSSS... SORRY! Your Browser did NOT support WebSocket");
        input.hide(500);
        return;
    }
    else{
        writeLogbook("PASSED : Your Browser DID support WebSocket");
    }


    // open connection
    //THE ADDRESS
    serveraddress =  $('#wsaddress').val();
    serverport =  $('#wsport').val();
    wsid = $('#wsid').val();
    wsgroup = $('#wsgroup').val();

    //creating the connection
    var wsurl = 'ws://'+serveraddress+':'+serverport;
    connection = new WebSocket(wsurl);

    writeLogbook('process... Connecting to  : ' + wsurl + ' using ID : '+ wsid + ' @ ' + wsgroup);


    //tell me what to do when it is connected
    connection.onopen = function () {
        // move the cursor to input text
        input.show(500);
        input.focus();

        //compile the id to be sent to serve
         
         
        $('#wsdisconnect').show(500);
        $('#wssetting').hide(500);
        //displaying the status
        writeLogbook('Connection with WEBSOCKET Server is Established, send any data now! ');


        sendDataWS("id", "IDENTITY");
    };

    //adding the listener when reveiving any messages here... 
    connection.onmessage = function (message) {

       receiveDataWS(message);
    
    };

    //adding the listener when Error
    connection.onerror = function (error){

      handleWSError(error);

    }

}

//**************************************
//  handleWSError, what to do if error Happens
//***************************************
function handleWSError(error){
  writeLogbook("ERROR ON WEBSOCKET CONNECTION ");
  writeLogbook("REASON : " + error);
}



//**************************************
//  sendDataWS, used to send data from client 
//  to the server
//***************************************
function sendDataWS(dataType, dataMessage){
  // Construct a msg object containing the data the server needs to process the message from the chat client.
  var msg = {
    "type": dataType,
    "text": dataMessage,
    "id":   wsid,
    "group" : wsgroup,
    "time": Date.now()
  };

  connection.send(JSON.stringify(msg));
}

//**************************************
//  sendIDWS, used to send data about ID after 
//  first ws connection is initiated
//***************************************
function sendIDWS(theid, thegroup){
  // Construct a msg object containing the data the server needs to process the message from the chat client.
  var msg = {
    "type": 'id',
    "text" : "",
    "id":   theid,
    "group" : thegroup,
    "time": Date.now()
  };

  connection.send(JSON.stringify(msg));
}

//**************************************
//  receiveDataWS, determining what to do  
//  with the message when RECEIVING THE DATA
//***************************************

function receiveDataWS(message){

  var rawdata = message.data;

  //to do with the received message is here
  //do as you wish with the data
  var data = JSON.parse(rawdata);
  writeLogbook('[' + data.time + '  from ' + data.id + '@' + data.group + '] '+ data.text );

  //do as you wish with your data here... 

}



//***************************************
// Use this to DISCONNECT From WS Server
//***************************************
function disconnectWS(){
  connection.close();
  writeLogbook('Disconnecting success....! ');
  $('#wsdisconnect').hide(500);
  $('#wssetting').show(500);

}

