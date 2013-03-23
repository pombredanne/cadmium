var client_id = "";
var current_code = "";
var access_token = "";
var sites = [];
var site_queue = [];
var deployed_wars = {};
var status_cache = {};
var initializing = false;

function setCookie(token){ 
    
   var date = new Date(); 
   date.setHours(date.getHours()+1); 
   expires = date.toUTCString(); 
   final_cookie = "access_cookie=" + encodeURIComponent(token) + ";expires_on = " + expires; 
   document.cookie = final_cookie; 
 
} 
 
function getCookie(cookie){ 
 
   var search_cookie = cookie + "="; 
    
   if(document.cookie.length > 0)
   { 
      start_position = document.cookie.indexOf(cookie) 
      if(start_position != -1)
      { 
         start_position += cookie.length;
         end_position = document.cookie.indexOf(";", start_position);
         if (end_position == -1) 
            end_position = document.cookie.length 
         return (decodeURIComponent(document.cookie.substring(start_position+1, end_position))) 
      } 
   } 
 
} 

function getUser(access_token){

  var user = "";
  $.ajax({
     url: 'https://api.github.com/user',
     async: false,
     headers: { 'Authorization': 'token '+access_token },
     contentType: 'application/json; charset=utf-8',
     dataType: 'json',
     success: function (data){
         user = data['login'];
     }
  });

  return user;
}

function signIn()
{
   
   uri = window.location;
   var regex = /(^.+code=)(\w+)$/i;
   var matchResult = regex.exec(uri);
   if(matchResult) {
     current_code = matchResult[2];
   }
   if(current_code) {
     $.ajax({ 
       url: 'system/github',
       async: false,
       success: function(data){
         client_id = data['clientId'];
           getAccessToken(client_id);
         window.location.replace('/');
       },
       error: function(data){
         console.log('Error getting client_id: '+data);
       }
     });
   } else {
     access_token = getCookie('access_cookie');
   }
}    

function redirectGithub() {
  $.ajax({ 
    url: 'system/github',
    async: false,
    success: function(data){
      client_id = data['clientId'];
      console.log('client_id: '+client_id);
      window.location.replace("https://github.com/login/oauth/authorize?client_id="+
      client_id+"&scope=repo"+"&redirect_uri=");
    }
  });
}

function displaySigninNotificationModal() {
  $('#sign-in-alert').css({'opacity': '1', 'pointer-events': 'auto'}); 
}

function getAccessToken(client_id){

   $.ajax({
      type: 'POST',
      async: false,
      url: '/system/github/accessToken',
      data: JSON.stringify({ 'clientId':client_id, 'code':current_code }),
      contentType: 'application/json; charset=utf-8',
      dataType: 'json',
      success: function(data){
          access_token = data['accessToken'];
        if(access_token != null){
          setCookie(access_token);
          }
      },
      error: function(data){
        console.log('Error getting access token: '+data);
      }
   });
}

function loadSites() {
  initializing = window.setInterval(function(){checkForDone();}, 500);
  console.log("initializing cadmium site list.");
  $.ajax({
    url: '/data/site-list.json',
    type: 'GET',
    dataType: 'json',
    success: function(sites) {
      updateProgress(5);
      if(sites['cadmium-sites']) {
        for(var i=0; i<sites['cadmium-sites'].length; i = i + 1) {
          console.log("Loading cadmium sites ["+sites['cadmium-sites'][i]+"]");
          getWarList(sites['cadmium-sites'][i], sites['cadmium-sites'].length);
        }
      }
    }
  });
}

function getWarList(domain, numSites){
  var progressAddition = Math.floor( 20 / numSites );
  $.ajax({
    type: 'GET',
    url: 'https://'+domain+'/system/deployment/list',
    headers: { 'Authorization': 'token ' + access_token },
    contentType: 'application/json; charset=utf-8',
    dataType: 'json',
    success: function (warList){
      incProgress(progressAddition);
      if(warList) {
        for(var i=0; i<warList.length; i = i + 1) {
          var progAmt = Math.floor( Math.floor( 40 / numSites ) / warList.length);
          console.log("Discovered deployed cadmium war ["+warList[i]+"]: "+progAmt);
            getWarInfo( domain, warList[i],  progAmt);
        }
      }
    },
    error: function() {
      var amt = progressAddition + Math.floor( 40 / numSites );
      incProgress(amt);
    }
  });
}

function getWarInfo( domain, warName, incAmt ){
  $.ajax({
    type: 'GET',
    url: 'https://'+domain+'/system/deployment/details/'+warName,
    headers: { 'Authorization': 'token ' + access_token },
    contentType: 'application/json; charset=utf-8',
    dataType: 'json',
    success: function (warInfo){
      incProgress(incAmt);
      sites.push(warInfo[ 'domain']);
      sites.sort();
      deployed_wars[warInfo[ 'domain' ]] = warInfo;
      getSiteStatus(warInfo[ 'domain' ], incAmt);
    },
    error: function () {
      incProgress(Math.floor(incAmt * 2));
      console.log("Failed to get warInfo for "+domain);
    }
  });
}

function getSiteStatus(domain, incAmt){
  $.ajax({
    type: 'GET',
    url: 'https://'+domain+'/system/status',
    headers: { 'Authorization': 'token '+access_token },
    contentType: 'application/json; charset=utf-8',
    dataType: 'json',
    success: function ( statusResp ){
      incProgress(incAmt);
      status_cache[ domain ] = statusResp;
      site_queue.push( domain );
    },
    error: function () {
      incProgress(incAmt);
      console.log("Failed to get status for "+domain);
    }
  });
}

function updateSiteStatus(domain, runAsAsync) {
  var newStatus = false;
  $.ajax({
    type: 'GET',
    url: 'https://'+domain+'/system/status',
    headers: { 'Authorization': 'token '+access_token },
    dataType: 'json',
    async: runAsAsync,
    success: function ( statusResp ){
      status_cache[ domain ] = statusResp;
      newStatus = statusResp;
      console.log("Updated status: "+domain);
    }
  });
  console.log("Returning updated status: "+newStatus);
  return newStatus;
}

function checkForDone() {
  var domain = site_queue.pop();
  if(domain) {
    iterations_since_last_new_site = 0;
  } else {
    if(typeof iterations_since_last_new_site !== 'undefined') {
      if(iterations_since_last_new_site >= 5) {
        window.clearInterval(initializing);
        console.log("Done initializing!");
        updateProgress(false);
        for(var i = 0; i<sites.length; i = i + 1) {
          addSite(sites[i]);
        }
        setupDragging();
        populateFilter();
        completeInitialization();
      } else {
        iterations_since_last_new_site++;
        console.log("iterations since last new site: "+iterations_since_last_new_site);
      }
    }
  }
}

function setupDragging() {
  $(".drag").draggable({
    appendTo: "body",
    helper: "clone"
  });
  $("#environments > section").droppable({
    drop: function( event, ui ) {
      var sourceDomain = $(ui.draggable).attr('data-id');
      var targetEnv = $(this).attr("data-env");
      var sourceWarInfo = deployed_wars[sourceDomain];
      var sourceStatus = status_cache[sourceDomain];
      if(sourceWarInfo && sourceStatus && sourceStatus['environment'] !== targetEnv) {
      console.log("Dropped "+sourceDomain+" on "+targetEnv);
        for( var domain in deployed_wars ) {
          var targetWarInfo = deployed_wars[domain];
          var targetStatus = status_cache[domain];
          if (targetStatus['environment'] === targetEnv) {
            if (targetWarInfo['repo'] == sourceWarInfo['repo']) {
              if(targetWarInfo['contentBranch'] === sourceWarInfo['contentBranch']) {
                $('[data-id="'+domain+'"]').addClass('pending');
                $('[data-id="'+sourceDomain+'"]').addClass('editable');
                confirmMessage(sourceDomain+' '+sourceStatus['branch'], sourceStatus['environment'], targetEnv);
                break;
              }
            }
          }
        }
      }
    }
  });
}

function incProgress(amt) {
  var progVal = $("#progressbar").progressbar( "value" );
  updateProgress( Math.floor(progVal) + Math.floor(amt));
}

function updateProgress(val) {
  $("#progressbar").progressbar({ value: val });
}

function completeInitialization() {
  $("#progressbar").progressbar({ value: 100 });
  $('#progress-modal').css({'opacity': '0', 'pointer-events': 'none'});
}

function addSite( domain ) {
  var warInfo = deployed_wars[ domain ];
  var status = status_cache[ domain ];
  if(status) {
    ensureEnvironmentAvailable(status['environment']);
    if(warInfo) {
      addSiteSection(warInfo, status, $('#' + status['environment']));
    } else {
      console.log("No warInfo["+warInfo+"] found for domain "+domain);
    }
  } else {
    console.log("No status["+status+"] found for domain "+domain);
  }
}

function ensureEnvironmentAvailable(env) {
  var envDiv = $('#environments');
  var envSection = $('#' + env);
  if(envSection.length == 0) {
    var sections = $('section', envDiv);
    var newSection = $('<section data-env="'+env+'"><h2>'+toTitleCase(env)+'</h2><div id="'+env+'"></div></section>');
    envDiv.append(newSection);
  }
}

function addSiteSection( warInfo, status, envSection ) {
  $.ajax({
    url: '/posts/site-block.html',
    type: 'GET',
    async: false,
    dataType: 'html',
    success: function( html ) {
      var newSite = $(html);
      newSite.attr("data-id", warInfo['domain']);
      $(".site-name", newSite).text(warInfo['domain']);
      $(".branch-name", newSite).text(status['branch']);
      $(".url a", newSite).attr("href", "http://"+warInfo['domain']);
      $(".url a", newSite).text("http://"+warInfo['domain']);
      envSection.append(newSite);
    }
  });
}

function getSiteHistory( domainlist, access_token ){

   var info = [];
   for(var i = 0; i < domainlist.length; i++)
   {
      $.ajax({
         type: 'GET',
           async: false,
         url: 'https://'+domainlist[i]+'/system/history',
         headers: { 'Authorization': 'token '+access_token },
         contentType: 'application/json; charset=utf-8',
         dataType: 'json',
         success: function ( data ){
        for( var j in data ){
           info.push(data[j]);
        }
         },
         error: function (data){
            console.log('Failed to get site ['+domainlist[i]+'] status.');
         }
      });    
   }
   return info;
}

function displayElements(domainlist, siteInfo){

      for(var i = 0; i < domainlist.length; i++){
          var domain = domainlist[i];
          if(domain.indexOf('.') != -1) domain = domain.substring(0, domain.indexOf('.'));
          $('#repo').append('<option>'+domain+'</option>');
          for(var j in siteInfo){
             $('#branch').append('<option>all branches</option>');
             $('#branch').append('<option>'+siteInfo[j]['branch']+'</option>');
             $('#dev').append('<ul class="release editable drag"><a href="#" class="delete" title="delete">x</a>'
                +'<li class="branch-name">'+siteInfo[j]['branch']+'</li>'
                +'<li class="commit">'+siteInfo[j]['revision'].substring(0, 10)+'</li>'
                +'<li class="url"><a href=https://'+siteInfo[j]['domain']+'>'+siteInfo[j]['domain']+'</a></li>'
                +'<li class="deployed">'
                    +'<span class="time">'+(new Date(siteInfo[j]['timestamp'])).toUTCString()+'</span>'
                    +'<span class="author"> by lynnandtonic</span></li></ul>'
             ); 
          } 
       }

}

function getLog(logInfo){

   var branch = $('#branch').children('option:selected').text();
   var repo = $('#repo').children('option:selected').text();

   $('#current-log').html(repo+'<span>'+branch+'</span>');
   $('#log').empty();

   for(var i = 0; i < logInfo.length && i < 5; i++){
      if( logInfo[i]['branch'] == branch || branch == 'all branches' )
      {
         $('#log').append('<li><span class="commit">'+logInfo[i]['revision'].substring(0, 10)+'</span> : user ... '
            +logInfo[i]['branch']+' on '+(new Date(logInfo[i]['timestamp'])).toUTCString()+'</li>'); 
      }
   }

}

//Show alert
function confirmMessage( elementName, originalDiv, newDev ){
  $('#alert span.name').text( elementName );  
  $('#alert span.original').text( originalDiv );
  $('#alert span.new').text( newDev );
  $('#alert').css({'opacity': '1', 'pointer-events': 'auto'}); 
}

function cloneSite( sourceDomain, targetDomain ) {
  updateSiteStatus(sourceDomain, false);
  var sourceStatus = status_cache[sourceDomain];
  var targetStatus = status_cache[targetDomain];
  console.log("Cloning "+sourceDomain+" ["+sourceStatus['repo']+":"+sourceStatus['branch']+":"+sourceStatus['revision']+"] to "+targetDomain);
  $.ajax({
    url: "https://"+targetDomain+"/system/update",
    type: "POST",
    headers: { 'Authorization': 'token '+access_token },
    contentType: 'application/json; charset=utf-8',
    dataType: 'json',
    cache: false,
    data: JSON.stringify({
       'repo':sourceStatus['repo'],
       'branch':sourceStatus['branch'],
       'sha':sourceStatus['revision'],
       'comment':'Cloned from cadmium admin.'
          }),
    success: function(data){
      if(data['message'] === 'ok') {
        if(data['uuid']) {
          waitFinished( sourceDomain, targetDomain, data['uuid'], data['timestamp'] );
        } else {
          endUpdate();
        }
      } else {
        failUpdate();
      }
    }, 
    error: function(data){
      failUpdate();
    }
  });
}

function endUpdate() {
  $('.editable').removeClass('editable');
  $('.pending').removeClass('pending');
}

function failUpdate() {
  endUpdate();
  console.log("Clone failed!!!");
  alert("The clone you requested has failed.");
}

function waitFinished( sourceDomain, targetDomain, uuid, timestamp ) {
  $.ajax({
    url: 'https://'+targetDomain+'/system/history/'+uuid+"/"+timestamp,
    type: 'GET',
    dataType: 'html',
    cache: false,
    success: function(response) {
      if(response === 'true') {
        endUpdate();
        alert("Success: "+sourceDomain+" is cloned to "+targetDomain);
        updateSiteStatus(targetDomain, true);
      } else {
        window.setTimeout(function() {
          waitFinished( sourceDomain, targetDomain, uuid, timestamp );
        }, 1000);
      }
    },
    error: function() {
      failUpdate();
    }
  });
}

function toTitleCase(str)
{
    return str.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
}

function populateFilter() {
  var filterBox = $("#repo");
  $("#repo").change(function (event) {
    console.log("Filtering by "+$(event.target).val());
    hideOthers($(event.target).val());
  });
  var repos = [];
  for(var domain in deployed_wars) {
    var warInfo = deployed_wars[domain];
    if(repos.indexOf(warInfo['repo']) == -1) {
      repos.push(warInfo['repo']);
    }
  }
  if(repos.length > 0) {
    var projects = {};
    var projectNames = [];
    for(var i=0; i<repos.length; i = i + 1) {
      var repoName = repos[i];
      if(repoName.indexOf(":") > -1) {
        repoName = repoName.split(":")[1];
      }
      if(repoName.indexOf(".git") > -1) {
        repoName = repoName.substring(0, repoName.indexOf(".git"));
      }

      var splitRepoName = repoName.split('/');
      repoName = splitRepoName[splitRepoName.length-2]+"/"+splitRepoName[splitRepoName.length-1];
      $.ajax({
        url: "https://api.github.com/repos/"+repoName,
        headers: {'Authorization': 'token '+access_token},
        async: false,
        dataType: 'json',
        success: function (data) {
          projectNames.push(data['name'].toLowerCase());
          projects[data['name'].toLowerCase()] = repos[i];
        }
      });
    }
    projectNames.sort();
    for(var i = 0; i < projectNames.length; i = i + 1) {
      filterBox.append($("<option value=\""+projects[projectNames[i]]+"\">"+projectNames[i]+"</option>"));
    }
  }
}

function showAllRepo() {
  $('[data-id]').show();
}

function hideOthers(repo) {
  var showDomain = [];
  if(repo !== 'show-all') {
    for( var domain in deployed_wars ) {
      if(deployed_wars[domain]['repo'] === repo) {
        showDomain.push(domain);
      }
    }
  }
  if(showDomain.length == 0) {
    showAllRepo();
  } else {
    $('[data-id]').each(function (){
      var me = $(this);
      if(showDomain.indexOf(me.attr('data-id')) == -1) {
        me.hide();
      } else {
        me.show();
      }
    });
  }
}


