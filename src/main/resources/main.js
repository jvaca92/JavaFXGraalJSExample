(function ($) {
    var GLOBAL_OBJECTS = FX_OBJECTS;
    $ = function(val) {
        if(val.split("")[0] == "#") {
            return GLOBAL_OBJECTS.get(val);
        } else if(val.split("")[0] == "."){
           //TODO - find solution to invoke method for more objects which are classified by class attribute
        }
    }
    var fxQuery = function(val) {
       if(val.split("")[0] == "#") {
                 return GLOBAL_OBJECTS.get(val);
             } else if(val.split("")[0] == "."){
                //TODO - find solution to invoke method for more objects which are classified by class attribute
             }
    }

    $('#btnMessage').setOnAction(function() {
        $("#lbMsg").text = "Hello there !!! This is JavaFX with GraalJS test !!!";
    })

    fxQuery('#btnProcess').setOnAction(function() {
         fxQuery("#pi").opacity = 100.0
    })

}());