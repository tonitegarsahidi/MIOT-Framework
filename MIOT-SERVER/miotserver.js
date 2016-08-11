// http://ejohn.org/blog/ecmascript-5-strict-mode-json-and-more/
"use strict";

// *****************************************
//          SERVER CONFIGURATION 
// *****************************************

// Optional. You will see this name in eg. 'ps' or 'top' command
process.title = 'miot-ws-server';


//websocket port
var HTTPport = 1337;

//DATABASE CONFIG
var usedatabase = false;

var dbname = "miot";
var dbserver = "localhost";
var dbuser = "root";
var dbpass = "tonitegar";

var dbtablename = "datasave";


//make this as true,if you want the message you send, autoforwarded returned to you again... 
var selfforward = true;

//verbose vs quiet
//set this to false, to minimize display on console log
var verbose = true;


// *****************************************
//          PRE DEFINER VARIABLES
//   this part is needed variable to keep up
// *****************************************

//define requirement
var WebSocketServer = require('websocket').server;
var http = require('http');


//mysql pre
var mysql = require("mysql");

// First you need to create a connection to the db
var con = mysql.createConnection({
  host: dbserver,
  user: dbuser,
  database: dbname,
  password: dbpass
});


//PRECONFIG, IF DATABASE IS USED... 
if(usedatabase==true){
    con.connect(function(err){
        if(err){
            console.log('MYSQL Error connecting to Db');
            return;
        }
        
        console.log('MYSQL Connection established');
    });
}

// list of currently connected clients (users)
var clients = [ ];      //for connection object
var clientsid = [];     //for id name
var clientsgroup = [];  //for groups name


/**
 * Helper function for escaping input strings
 */
function htmlEntities(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;')
                      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}


//=================== starting the HTTP SERVER========= ==============
var HTTPserver = http.createServer(function(request, response) {
    // process HTTP request. Since we're writing just WebSockets server
    // we don't have to implement anything.
});

//starting the HTTPserver is here
HTTPserver.listenWW(HTTPport, function() { 
	console.log('MIOT SERVER IS RUNNING...');
	console.log('listening on port ' + HTTPport);
});


// ============= THIS PART IS CREATING THE WEBSOCKET SERVER SODARA2 ===============
// create the server
var wsServer = new WebSocketServer({
    httpServer: HTTPserver
    // WebSocket server is tied to a HTTP server. WebSocket request is just
    // an enhanced HTTP request. For more info http://tools.ietf.org/html/rfc6455#page-6
});




// This callback function is called every time someone
// tries to connect to the WebSocket server
wsServer.on('request', function(request) {
	//display logs
	console.log((new Date()) + ' Connection from origin ' + request.origin + '.');

	// accepting connection - you should check 'request.origin' to make sure that
    // client is connecting from your website
    var connection = request.accept(null, request.origin); 
    //console.log("Connection : " + connection);

    // we need to know client index to remove them on 'close' event
    var index;
    var indexid;
    var indexgroup;

    var firstmessage = true;

    // tell in log that connection is already accepted
    console.log((new Date()) + ' Connection is accepted.');

    //this callback is when users sent some message
    connection.on('message', function(message) {

        //CHECKING TO SEE IF THE PAYLOAD SEND IS IN STRING UTF8 FORMAT
        if (message.type === 'utf8'){
            
            var json;    
            //get the utf8 text (String) since all data is all through stringify  
            var payload = message.utf8Data;

            //handle json
            try {
                
                json = JSON.parse(payload);
                
                 // *****************************************
                 // EXTRACTING THE DATA
                 // this part we extract the data on the server
                 // you may skip this part, by simply commenting.. 
                 // *****************************************

                var messageType   = json.type;
                var messageData   = json.text;
                var messageSender = json.id;
                var messageGroup  = json.group;
                var messageTime   = json.time;
                
                if(firstmessage==true){

                  //console.log("this is the first message");
                  
                  //console.log("change first message to false");
                  firstmessage = false;

                  //know the index of the groupee
                  index = clients.push(connection) - 1;
                  indexid = clientsid.push(messageSender) - 1;
                  indexgroup = clientsgroup.push(messageGroup) - 1;

                }

                //the first message is always an empty message with Id.
                if(messageType == 'id'){
                    console.log("Hello " + messageSender);
                }
                else{

                

                    // *****************************************
                     // DATABSE HANDLE IS HERE
                     // put every received message into database 
                     // *****************************************
                     if(usedatabase == true){
                        

                        var dbsave = { senderid: messageSender, sendergroup:messageGroup, msgtype:messageType, msgcontent:messageData, msgtimestamp:messageTime, timearrived:Date.now()};
                        var query = 'INSERT INTO '+ dbtablename + ' SET ?'; 

                        con.query(query, dbsave, function(err,res){
                            if(err) throw err;

                            console.log('MYSQL : message inserted to DB with ID:', res.insertId);

                            //con.end();

                        
                        });

                     }


                     // *****************************************
                     // YOUR SERVER CODE ABOUT DATA HANDLE IS HERE
                     // e.g. put on database, modify data, etc 
                     // *****************************************

                     // put your code here


                     if(verbose == true){
                        console.log((new Date()) + ' Received Message from '
                                        +'['+ messageSender + ' @ '+ messageGroup + ' on ' + messageTime +']' +': ' + messageData );  
                     }
                    

                    // *****************************************
                    // MIOT FORWARDER IS HERE
                    // broadcast message to all connected clients
                     // *****************************************
                    for (var i=0; i < clients.length; i++) {
                        if(messageGroup == clientsgroup[i]){

                          //if selfforward is set to true (sender also receive his own   message)
                          if(selfforward==true){

                            //forward the payload to current clients
                            clients[i].sendUTF(payload);
                            
                            //display into log
                            if(verbose == true){
                              console.log("Forwarding to " + clientsid[i] + ' @ '+ clientsgroup[i]);
                            }
                            
                          }
                          //if sender doesn't want to receive his own message, but others can...
                          else if(selfforward == false && messageSender != clientsid[i] ){
                            clients[i].sendUTF(payload);
                            
                            //display into log
                            if(verbose == true){
                              console.log("Forwarding to " + clientsid[i] + ' @ '+ clientsgroup[i]);
                            }

                            
                          }
                          else{

                            //display into log
                            if(verbose == true){
                              console.log("skipping forward message to sender");
                            }

                            
                          }
                          

                        }

                    }

                }

            } catch (e) {   //if error in parsing json happen
                console.log('This doesn\'t look like a valid JSON: ', payload);
                return;
            }
        }   //end if

        else{   //if the message is not in utf8 string encoded  format... 
            
            console.log("We got NON UTF8 message : " + message);
        }
        

    });

	
    // *****************************************
    //         WHEN A USER IS DISCONNECTED
    // *****************************************
    connection.on('close', function(connection) {
            
            //write on console about the status
            console.log((new Date()) + " Peer "
                + connection.remoteAddress + " disconnected.");
            //say goodbye
            console.log(" see you again, "+ clientsid[index] + ' @ ' + clientsgroup[index]);

            // remove user from the list of connected clients
            clients.splice(index, 1);
            clientsid.splice(index, 1);
            clientsgroup.splice(index, 1);
        
    });
    
});