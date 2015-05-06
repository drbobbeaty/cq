/*
 * Function to reload the data in the 'last-import-times' table on the
 * main page. This will clear out what's there, and hit the endpoint that
 * will bring back all the data it needs to properly make the page.
 */
function solveThePuzzle() {
  console.log("attempting to solve the puzzle");
  // get what we need for the call...
  var inp = {};
  inp.cyphertext = $("#plaintext").val();
  inp.clue = {};
  inp.clue[$("#key1").val()] = $("#key2").val();
  // make the call to solve the puzzle
  $.ajax({type: "POST",
          url: "/solve",
          processData: false,
          contentType: 'application/json',
          data: JSON.stringify(inp),
          success: function(resp) {
            var cont = '<div class="alert alert-success" role="alert">';
            cont += '<strong>Solved:</strong> ' + resp.plaintext;
            cont += '</div>';
            $("#status").replaceWith(cont);
          }
  })
}
