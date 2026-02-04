//jquery-click-scroll
//by syamsul'isul' Arifin

var sectionArray = [1, 2, 3, 4, 5];

$.each(sectionArray, function(index, value){
     var sectionId = '#' + 'section_' + value;
     var $section = $(sectionId);
     if ($section.length === 0) {
         return;
     }

     $(document).scroll(function(){
         var offsetSection = $section.offset().top - 94;
         var docScroll = $(document).scrollTop();
         var docScroll1 = docScroll + 1;
         
        
         if ( docScroll1 >= offsetSection ){
             $('.navbar-nav .nav-item .nav-link').removeClass('active');
             $('.navbar-nav .nav-item .nav-link:link').addClass('inactive');  
             $('.navbar-nav .nav-item .nav-link').eq(index).addClass('active');
             $('.navbar-nav .nav-item .nav-link').eq(index).removeClass('inactive');
         }
         
     });
    
    $('.click-scroll').eq(index).click(function(e){
        var offsetClick = $section.offset().top - 94;
        e.preventDefault();
        $('html, body').animate({
            'scrollTop':offsetClick
        }, 300)
    });
    
});

$(document).ready(function(){
    $('.navbar-nav .nav-item .nav-link:link').addClass('inactive');    
    $('.navbar-nav .nav-item .nav-link').eq(0).addClass('active');
    $('.navbar-nav .nav-item .nav-link:link').eq(0).removeClass('inactive');
});
