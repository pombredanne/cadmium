var access_token = "";
var domainlist = [];
var siteInfo = [];
var logInfo;

$(function(){    
  signIn();  
  $('#sign-in-btn').click(function() {
    $('#sign-in-alert').css({'opacity': '0', 'pointer-events': 'none'});
    redirectGithub();
  });

  if( !getCookie("access_cookie") ) {
    displaySigninNotificationModal();
  } else {
    var user = getUser(getCookie("access_cookie"));
    if(!user) {
      displaySigninNotificationModal();
    } else {
      $("#logged-in span").text(user);
      $("#logged-in").show();
       
      $("#progressbar").progressbar({ value: 0 });
      $('#progress-modal').css({'opacity': '1', 'pointer-events': 'auto'});
      loadSites();
    

       var uri = window.location+"";
/*
       if(uri.indexOf('#callback') >= 0){
           signIn();
       }

       if( !getCookie("access_cookie") && uri.indexOf('#callback') <= 0 ){
      return false;
       }
       else{
      $('span', '#sign-in').toggle();
      access_token = getCookie("access_cookie");
      $('#user').html('Logged in as: <strong>'+getUser(access_token)+'</strong>');
       }

       domainlist = getWarList(access_token);
       siteInfo = getSiteStatus(domainlist, access_token);

       //populate filter menus and environment elements
       displayElements(domainlist, siteInfo);

       //get log info and display the first (default) element
       var logInfo = getSiteHistory(domainlist, access_token);
       console.log(logInfo);
       getLog(logInfo);
*/
       //Needs work
       $('#environments > section').hover(
      function () {
         if( $('.dragging').length > 0 ) { 
            $(this).height( $(this).outerHeight(true) + $('.dragging').outerHeight(true) );
         }
      },
        function () {
         if($('.dragging').length > 0){
            if( $('.dropped').length > 0 ){
           $('.dragging').removeClass('dragging');
           $('.dropped').removeClass('dropped');
            }
        if($('.first').length == 0 && $('.dragging').length > 0){
           $(this).height( $(this).outerHeight(true) - $('.dragging').outerHeight(true) );
           $(this).children('ul').css({'margin':'4%'});
            }
        //handles dragging out of the original environment
        if($('.first').length > 0){
           $('.first').removeClass('first');
        }
         }
      }
       );

       //Make element draggable
       $(".drag").draggable({
      start: function(event, ui){
         $(this).addClass('dragging');
         $(this).addClass('first');
      },
          helper: 'clone',
      cursor: 'move',
      stop: function (event, ui){
         $(this).addClass('dropped');
         $(this).draggable('disable');
      }
       });

       //Make element droppable
       $("#environments > section").droppable({
      drop: function( event, ui ){
         var element = $(ui.draggable).clone();
         $(this).append(element);
         $(element).addClass('pending'); 
         $(element).removeClass('drag');
         $(element).addClass('confirm');
         confirmMessage( $(ui.draggable).children('li.branch-name').text(), $(ui.draggable).parents('section').children('h2').text(), $(this).parents('section').children('h2').text() );
      }
       });

       

       $("#view-log").click(function() {
       $('#log').toggle();
       $('span', this).toggle();
       });

       $('#never-mind').click(function() {
       $('#alert').css({'opacity': '0', 'pointer-events': 'none'});
     $('.pending').removeClass('pending');
       $('.editable').removeClass('editable');
       });

       $('#do-it').click(function() {
           //Start the duplicating process here
         var sourceDomain = $(".editable").attr('data-id');
         var targetDomain = $(".pending").attr('data-id');
           $('.editable').removeClass('editable');
           $('#alert').css({'opacity': '0', 'pointer-events': 'none'});
         cloneSite(sourceDomain, targetDomain);
       });

       $('#branch').change( function() {
           //change elements in environments
       //update log
       getLog(logInfo);
       });

    }
  }
});
